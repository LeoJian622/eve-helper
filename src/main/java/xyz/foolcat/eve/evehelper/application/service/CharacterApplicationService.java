package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;

/**
 * 角色应用服务
 * 负责角色 ESI 授权绑定用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterApplicationService {

    private final EsiGateway esiApiService;

    /**
     * 使用授权 code 绑定角色到当前用户。
     *
     * @param code   授权 code
     * @param userId 用户 ID
     */
    public void authorizeCharacter(String code, Integer userId) {
        try {
            esiApiService.authorize(code, userId);
        } catch (ParseException e) {
            log.error("角色授权失败:  code={}",  code, e);
            throw new EveHelperException("角色授权失败", e);
        }
    }

    /**
     * 查询指定角色访问 ESI 所需的 accessToken。
     *
     * <p><b>所有权校验</b>:仅当 {@code (userId, characterId)} 在角色绑定表中精确命中一行时才返回。
     * 不接受军团(corpId)维度放行,ROOT/ADMIN 亦不豁免 —— accessToken 是可直接冒用的 bearer 凭证,
     * 与「查看数据」性质不同,最小权限优先于与其它接口的一致性。</p>
     *
     * <p>参数守卫前置于任何外部调用与查库:未认证时 {@code UserUtil.getUserId()} 返回 -1,
     * 此处显式拒绝而非依赖「库中无 user_id≤0 的行」这一数据巧合(fail-closed)。</p>
     *
     * <p>本方法含 ESI 网络调用,不得置于数据库事务内。</p>
     *
     * @param characterId 角色 ID,须为正整数
     * @param userId      当前登录用户 ID,须为正整数
     * @return 含 accessToken、characterId 与剩余有效秒数的读模型
     * @throws EveHelperException 参数非法、未认证或无权访问该角色时抛出
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CharacterAccessTokenResult queryAccessToken(Integer characterId, Integer userId) {
        // 参数守卫先于一切外部调用:避免非法值穿透到下游查询
        if (characterId == null || characterId <= 0) {
            log.warn("角色AccessToken查询参数非法: userId={}, characterId={}", userId, characterId);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        // fail-closed:未认证(UserUtil 返回 -1)或主体无法识别一律拒绝
        if (userId == null || userId <= 0) {
            log.warn("角色AccessToken查询越权:未认证或主体无法识别 characterId={}", characterId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        try {
            CharacterAccessTokenResult result = esiApiService.getAccessTokenWithExpiry(characterId, userId);
            // 审计日志:只记标识与结果,绝不记 token 明文
            log.info("角色AccessToken查询成功: userId={}, characterId={}", userId, characterId);
            return result;
        } catch (EveHelperException e) {
            // 归属校验失败与 ESI 授权失效都走此分支;下游已统一错误码以消除存在性 oracle,此处仅补审计
            log.warn("角色AccessToken查询被拒: userId={}, characterId={}, code={}",
                    userId, characterId, e.getResultCode() == null ? null : e.getResultCode().getCode());
            throw e;
        } catch (ParseException e) {
            // 不记 code/token 等凭证内容,只记标识
            log.error("角色AccessToken解析失败: userId={}, characterId={}", userId, characterId, e);
            throw new EveHelperException("角色AccessToken获取失败", e);
        }
    }
}