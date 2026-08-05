package xyz.foolcat.eve.evehelper.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private final EveAccountService eveAccountService;

    private final EsiGateway esiApiService;

    private final EveAccountAssembler eveAccountAssembler;

    private final SysUserService sysUserService;

    private final SysUserAssembler userAssembler;

    private final PasswordEncoder passwordEncoder;

    private final Executor esiAuthStatusExecutor;

    public UserApplicationService(EveAccountService eveAccountService,
                                  EsiGateway esiApiService,
                                  EveAccountAssembler eveAccountAssembler,
                                  SysUserService sysUserService,
                                  SysUserAssembler userAssembler,
                                  PasswordEncoder passwordEncoder,
                                  @Qualifier("esiAuthStatusExecutor") Executor esiAuthStatusExecutor) {
        this.eveAccountService = eveAccountService;
        this.esiApiService = esiApiService;
        this.eveAccountAssembler = eveAccountAssembler;
        this.sysUserService = sysUserService;
        this.userAssembler = userAssembler;
        this.passwordEncoder = passwordEncoder;
        this.esiAuthStatusExecutor = esiAuthStatusExecutor;
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
        sysUserService.insert(sysUser);
    }

    /**
     * 获取用户绑定的所有角色(含 ESI 授权状态)。
     *      * <p>
     *      * 访问控制:仅允许查询本人账户;ROOT 角色(ADMIN)可查询任意用户(防御 IDOR,不依赖 DB url_perm 命名)。
     *      * ESI 网络调用置于事务外(NOT_SUPPORTED),DB 读写各自短事务。
     *      * 多角色状态判定并行执行,单角色异常被隔离为 UNKNOWN,不影响其他角色。
     *
     * @param userId 用户 ID
     * @return 带 ESI 授权状态的角色列表
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<UserAccountDTO> queryAccountListWithAuthStatus(Integer userId) {
        verifyAccess(userId);
        List<EveAccount> accounts = eveAccountService.getAccountList(userId);
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserAccountDTO> dtos = eveAccountAssembler.domain2UserAccountTO(accounts);
        List<CompletableFuture<EsiAuthStatus>> futures = new ArrayList<>(accounts.size());
        for (EveAccount account : accounts) {
            futures.add(CompletableFuture.supplyAsync(() -> esiApiService.getAuthorizationStatus(account), esiAuthStatusExecutor));
        }
        for (int i = 0; i < dtos.size(); i++) {
            dtos.get(i).setAuthStatus(resolveStatus(futures.get(i)));
        }
        return dtos;
    }

    /**
     * 校验访问权限:非本人且非 ROOT 角色拒绝访问(防御 IDOR)。
     * 系统内部调用(UserUtil.getUserId() <= 0)放行,由既有 RBAC 把控。
     */
    private void verifyAccess(Integer userId) {
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId > 0 && !currentUserId.equals(userId) && !isCurrentUserRoot()) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
    }

    /**
     * 当前认证用户是否为 ROOT 角色(ADMIN)。
     */
    private boolean isCurrentUserRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> GlobalConstants.ROOT_ROLE_CODE.equals(a.getAuthority()));
    }

    /**
     * 等待单角色状态判定结果,带超时与异常隔离(任何异常/超时归为 UNKNOWN)。
     */
    private EsiAuthStatus resolveStatus(CompletableFuture<EsiAuthStatus> future) {
        try {
            return future.get(STATUS_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return EsiAuthStatus.UNKNOWN;
        } catch (ExecutionException e) {
            return EsiAuthStatus.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return EsiAuthStatus.UNKNOWN;
        }
    }
}