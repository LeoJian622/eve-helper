package xyz.foolcat.eve.evehelper.application.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 访问控制守卫单元测试。
 * <p>
 * 固定两个方法的契约差异与参数校验顺序：requireOwnership 比对 EVE 人物/军团ID，
 * requireSelfOrRoot 比对系统用户ID；两者的参数校验都必须先于 ROOT 豁免，
 * 避免 ROOT 携带空值穿透到下游查询。
 *
 * @author Leojan
 * date 2026-08-07
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("访问控制守卫单元测试")
class AccessGuardUnitTest {

    private static final Integer CURRENT_USER_ID = 5;

    private static final Integer OWN_CHARACTER_ID = 2112818290;

    private static final Integer OWN_CORP_ID = 98000001;

    @MockBean
    EveAccountService eveAccountService;

    @Autowired
    private AccessGuard accessGuard;

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    private void loginAsJwt(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private void loginAsOwner() {
        loginAsJwt(CURRENT_USER_ID, "USER");
        EveAccount a = new EveAccount();
        a.setCharacterId(OWN_CHARACTER_ID);
        a.setCorpId(OWN_CORP_ID);
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(a));
    }

    // ---------- requireOwnership ----------

    @Test
    @DisplayName("归属校验：本人角色 -> 放行")
    void requireOwnership_ownCharacter_passes() {
        loginAsOwner();

        assertDoesNotThrow(() ->
                accessGuard.requireOwnership(String.valueOf(OWN_CHARACTER_ID), "测试资源"));
    }

    @Test
    @DisplayName("归属校验：本人军团 -> 放行（corp 维度归属）")
    void requireOwnership_ownCorp_passes() {
        loginAsOwner();

        assertDoesNotThrow(() ->
                accessGuard.requireOwnership(String.valueOf(OWN_CORP_ID), "测试资源"));
    }

    @Test
    @DisplayName("归属校验：他人角色 -> 拒绝")
    void requireOwnership_otherCharacter_throws() {
        loginAsOwner();

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership("90000001", "测试资源"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("归属校验：未认证 -> 拒绝，且不查归属")
    void requireOwnership_unauthenticated_throws() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership(String.valueOf(OWN_CHARACTER_ID), "测试资源"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("归属校验：ownerId 为 null -> 抛参数错误（参数校验先于 ROOT 豁免）")
    void requireOwnership_nullOwnerId_throwsParamError() {
        loginAsJwt(CURRENT_USER_ID, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership(null, "测试资源"));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("归属校验：ROOT 传 null ownerId -> 抛参数错误，不得穿透到下游")
    void requireOwnership_rootWithNullOwnerId_throwsParamError() {
        loginAsJwt(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership(null, "测试资源"));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("归属校验：ROOT 传字符串 \"null\" -> 抛参数错误（String.valueOf(null) 的产物）")
    void requireOwnership_rootWithNullLiteral_throwsParamError() {
        loginAsJwt(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership("null", "测试资源"));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("归属校验：ROOT 访问他人 -> 放行，且不查归属")
    void requireOwnership_rootOther_passes() {
        loginAsJwt(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);

        assertDoesNotThrow(() -> accessGuard.requireOwnership("90000001", "测试资源"));

        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("归属校验：携带 ADMIN 权限但未认证的令牌 -> 拒绝")
    void requireOwnership_unauthenticatedAdminToken_throws() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE)));
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership(String.valueOf(OWN_CHARACTER_ID), "测试资源"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    // ---------- requireSelfOrRoot ----------

    @Test
    @DisplayName("本人校验：本人 -> 放行；他人 -> 拒绝")
    void requireSelfOrRoot_selfPassesOtherThrows() {
        loginAsJwt(CURRENT_USER_ID, "USER");

        assertDoesNotThrow(() -> accessGuard.requireSelfOrRoot(CURRENT_USER_ID, "测试资源"));

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireSelfOrRoot(999, "测试资源"));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("两个方法互不混用：系统用户ID 不应命中人物/军团归属")
    void twoMethodsAreNotInterchangeable() {
        loginAsOwner();

        // 把系统用户ID 当作 ownerId 传给归属校验：不匹配任何 characterId/corpId，应拒绝
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.requireOwnership(String.valueOf(CURRENT_USER_ID), "测试资源"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }
}
