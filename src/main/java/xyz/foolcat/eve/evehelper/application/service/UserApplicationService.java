package xyz.foolcat.eve.evehelper.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.request.ChangePasswordRequest;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.EsiAuthorizationDetail;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 用户应用服务
 *
 * @author Leojan
 * date 2026-07-31 16:08
 */

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserApplicationService {

    /**
     * 单角色状态判定结果等待上限,秒(覆盖 ESI 5s 超时 + 缓存 IO)
     */
    private static final long STATUS_FUTURE_TIMEOUT_SECONDS = 10;

    /**
     * 组合判定的兜底读模型:UNKNOWN + scopes null(超时/异常/中断/结果为 null 时统一归于此)。
     */
    private static final EsiAuthorizationDetail UNKNOWN_DETAIL =
            new EsiAuthorizationDetail(EsiAuthStatus.UNKNOWN, null);

    private final EveAccountService eveAccountService;

    private final EsiGateway esiApiService;

    private final EveAccountAssembler eveAccountAssembler;

    private final SysUserService sysUserService;

    private final SysUserAssembler userAssembler;

    private final PasswordEncoder passwordEncoder;

    private final Executor esiAuthStatusExecutor;

    private final AccessGuard accessGuard;

    private final SysUserRepository sysUserRepository;

    private final CacheGateway cacheGateway;

    private final TokenService tokenService;

    private final AppNotificationPushGateway pushGateway;

    /** 改密失败限流：最大尝试次数 */
    private static final int PWD_MAX_ATTEMPTS = 5;

    /** 改密失败限流：锁定分钟数 */
    private static final long PWD_LOCK_MINUTES = 30;

    public UserApplicationService(EveAccountService eveAccountService,
                                  EsiGateway esiApiService,
                                  EveAccountAssembler eveAccountAssembler,
                                  SysUserService sysUserService,
                                  SysUserAssembler userAssembler,
                                  PasswordEncoder passwordEncoder,
                                  @Qualifier("esiAuthStatusExecutor") Executor esiAuthStatusExecutor,
                                  AccessGuard accessGuard,
                                  SysUserRepository sysUserRepository,
                                  CacheGateway cacheGateway,
                                  TokenService tokenService,
                                  AppNotificationPushGateway pushGateway) {
        this.eveAccountService = eveAccountService;
        this.esiApiService = esiApiService;
        this.eveAccountAssembler = eveAccountAssembler;
        this.sysUserService = sysUserService;
        this.userAssembler = userAssembler;
        this.passwordEncoder = passwordEncoder;
        this.esiAuthStatusExecutor = esiAuthStatusExecutor;
        this.accessGuard = accessGuard;
        this.sysUserRepository = sysUserRepository;
        this.cacheGateway = cacheGateway;
        this.tokenService = tokenService;
        this.pushGateway = pushGateway;
    }

    /**
     * 修改密码（T041，R10 全会话失效链路）。
     * <p>
     * 流程：限流检查 → 验旧密 → 新旧相同拒绝 → 更新密码 → 写纪元 → 逐 sid 撤销+标记 → WS 关闭 → 清计数。
     * <p>
     * <b>必须覆盖类级 readOnly=true</b>：否则只读连接拒写。
     *
     * @param request 改密请求（旧密码 + 新密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        // 1. 认证守卫
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }

        // 2. 限流检查（先 get 计数 ≥5 → RATE_LIMITED，检查不 increment）
        String attemptKey = GlobalConstants.PWD_ATTEMPT_KEY_PREFIX + userId;
        int attempts = getCurrentAttempts(attemptKey);
        if (attempts >= PWD_MAX_ATTEMPTS) {
            throw new EveHelperException(ResultCode.RATE_LIMITED);
        }

        // 3. 查询用户
        SysUser user = sysUserRepository.queryById(userId);
        if (user == null) {
            throw new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }

        // 4. 验旧密（失败 increment + expire）
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            Long newCount = cacheGateway.increment(attemptKey);
            if (newCount != null && newCount == 1L) {
                cacheGateway.expire(attemptKey, PWD_LOCK_MINUTES, TimeUnit.MINUTES);
            }
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }

        // 5. 新旧相同拒绝（不 increment）
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }

        // 6. 更新密码
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserRepository.insertOrUpdateSelective(update);

        // 7. 写纪元（7 天 TTL）
        cacheGateway.set(GlobalConstants.REVOKE_EPOCH_KEY_PREFIX + userId, System.currentTimeMillis(), 7, TimeUnit.DAYS);

        // 8. 逐 sid 撤销 + 标记（markSessionRevoked 无条件调用，HIGH-1）
        Set<Object> sids = cacheGateway.sMembers(GlobalConstants.REFRESH_USER_KEY_PREFIX + userId);
        if (sids != null) {
            for (Object sid : sids) {
                String s = String.valueOf(sid);
                tokenService.revokeRefreshTokenBySession(s);
                tokenService.markSessionRevoked(s);
            }
        }

        // 9. WS 会话关闭
        pushGateway.closeUserSessions(userId);

        // 10. 清除计数
        cacheGateway.delete(attemptKey);
    }

    /**
     * 解析改密失败计数（仿 LoginRateLimiterService.getCurrentAttempts）。
     * null → 0；Integer/Long/toString 兼容解析。
     */
    private int getCurrentAttempts(String key) {
        Object attemptsObj = cacheGateway.get(key);
        if (attemptsObj == null) {
            return 0;
        }
        if (attemptsObj instanceof Integer number) {
            return number;
        } else if (attemptsObj instanceof Long number) {
            return number.intValue();
        } else {
            try {
                return Integer.parseInt(attemptsObj.toString());
            } catch (NumberFormatException e) {
                // MIN-3: 脱敏告警（镜像 LoginRateLimiterService:84-86）。
                // 术语更正：解析失败返回 0 是 fail-open（按 0 次尝试放行），非 fail-closed；
                // 镜像 LoginRateLimiterService 现状，行为零变更。
                log.warn("[SECURITY_ALERT] pwd attempt 计数解析失败, fail-open→0(按 0 次尝试放行): key={}", key);
                return 0;
            }
        }
    }

    /**
     * 用户注册:转换 DTO、加密密码并入库。
     *
     * @param user 注册信息
     */
    @Transactional
    public void register(UserDTO user) {
        SysUser sysUser = userAssembler.userDto2SysUser(user);
        if (sysUser == null) {
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        // SysUser.deleted 字段默认 true(将新用户标记为"已删除"),注册必须显式置 false,
        // 否则新用户可以注册却无法登录/被逻辑删除过滤
        sysUser.setDeleted(false);
        sysUserService.insert(sysUser);
    }

    /**
     * 获取用户绑定的所有角色(含 ESI 授权状态)。
     * <p>
     * 访问控制:仅允许查询本人账户;ROOT 角色(ADMIN)可查询任意用户(防御 IDOR,不依赖 DB url_perm 命名)。
     * 未认证或主体无法识别一律拒绝(fail-closed)。
     * ESI 网络调用置于事务外(NOT_SUPPORTED),DB 读写各自短事务。
     * 多角色状态判定并行执行,单角色异常被隔离为 UNKNOWN,不影响其他角色。
     *
     * @param userId 用户 ID
     * @return 带 ESI 授权状态的角色列表
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<UserAccountDTO> queryAccountListWithAuthStatus(Integer userId) {
        accessGuard.requireSelfOrRoot(userId, "用户角色列表");
        List<EveAccount> accounts = eveAccountService.getAccountList(userId);
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserAccountDTO> dtos = eveAccountAssembler.domain2UserAccountDTO(accounts);
        List<CompletableFuture<EsiAuthorizationDetail>> futures = new ArrayList<>(accounts.size());
        for (EveAccount account : accounts) {
            futures.add(CompletableFuture.supplyAsync(() -> esiApiService.getAuthorizationDetail(account), esiAuthStatusExecutor));
        }
        for (int i = 0; i < dtos.size(); i++) {
            EsiAuthorizationDetail detail = resolveDetail(futures.get(i));
            dtos.get(i).setAuthStatus(detail.status());
            dtos.get(i).setScopes(detail.scopes());
        }
        return dtos;
    }

    /**
     * 等待单角色组合判定结果(状态+scopes),带超时与异常隔离;
     * 任何异常/超时/结果为 null(实现返回 null 时)统一归为 UNKNOWN_DETAIL,不影响其他角色。
     */
    private EsiAuthorizationDetail resolveDetail(CompletableFuture<EsiAuthorizationDetail> future) {
        try {
            EsiAuthorizationDetail result = future.get(STATUS_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result != null ? result : UNKNOWN_DETAIL;
        } catch (TimeoutException e) {
            future.cancel(true);
            return UNKNOWN_DETAIL;
        } catch (ExecutionException e) {
            return UNKNOWN_DETAIL;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UNKNOWN_DETAIL;
        }
    }
}
