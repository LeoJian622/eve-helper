package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色应用服务单元测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("角色应用服务单元测试")
class CharacterApplicationServiceUnitTest {

    @MockBean
    EsiGateway esiApiService;

    @Autowired
    private CharacterApplicationService characterApplicationService;

    @Test
    @DisplayName("授权成功 -> 调用 ESI getAccessToken")
    void authorizeCharacter_success() throws Exception {
        characterApplicationService.authorizeCharacter("code123", 1);

        verify(esiApiService).authorize("code123", 1);
    }

    @Test
    @DisplayName("ESI 抛 ParseException -> 转 EveHelperException")
    void authorizeCharacter_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(esiApiService).authorize("code123", 1);

        assertThrows(EveHelperException.class,
                () -> characterApplicationService.authorizeCharacter("code123", 1));
    }

    // ── 006 feature:AccessToken 查询(T010 US1)与 userId 门禁(T013 US2,FR-012) ──

    private static final Integer USER_ID = 100;
    private static final Integer CID = 95465499;

    @Test
    @DisplayName("本人角色 -> 返回含 accessToken/characterId/expiresIn 的读模型")
    void queryAccessToken_ownCharacter_returnsResult() throws Exception {
        when(esiApiService.getAccessTokenWithExpiry(CID, USER_ID))
                .thenReturn(new CharacterAccessTokenResult("Bearer at", CID, 1140L));

        CharacterAccessTokenResult actual = characterApplicationService.queryAccessToken(CID, USER_ID);

        assertEquals("Bearer at", actual.accessToken());
        assertEquals(CID, actual.characterId());
        assertEquals(1140L, actual.expiresIn());
    }

    @Test
    @DisplayName("下游抛 ParseException -> 转 EveHelperException,不泄露 JWT 解析细节")
    void queryAccessToken_parseError_convertedToBusinessException() throws Exception {
        when(esiApiService.getAccessTokenWithExpiry(CID, USER_ID))
                .thenThrow(new ParseException("Missing part delimiters", 0));

        assertThrows(EveHelperException.class,
                () -> characterApplicationService.queryAccessToken(CID, USER_ID));
    }

    @Test
    @DisplayName("归属校验失败的业务异常原样上抛,错误码不被改写(保持不可区分性)")
    void queryAccessToken_ownershipFailure_rethrowsUnchanged() throws Exception {
        EveHelperException downstream = new EveHelperException(
                xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode.ESI_AUTHORIZATION_FAILURE);
        when(esiApiService.getAccessTokenWithExpiry(CID, USER_ID)).thenThrow(downstream);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> characterApplicationService.queryAccessToken(CID, USER_ID));

        assertSame(downstream, ex, "错误码与消息必须原样透传,否则破坏『属他人』与『不存在』的不可区分性");
    }

    @Test
    @DisplayName("userId 为 null / 0 / -1(未认证哨兵值) -> 拒绝且不调用 ESI 网关")
    void queryAccessToken_invalidUserId_rejectedWithoutGatewayCall() throws Exception {
        // UserUtil.getUserId() 未认证/主体无法识别时返回 -1。
        // 此前该值的拒绝仅依赖「库中无 user_id≤0 的行」这一数据巧合:
        // DDL 无 CHECK (user_id>0),一条 user_id=-1 的占位行即可让未认证请求拿到 token。
        for (Integer badUserId : new Integer[]{null, 0, -1}) {
            EveHelperException ex = assertThrows(EveHelperException.class,
                    () -> characterApplicationService.queryAccessToken(CID, badUserId),
                    "userId=" + badUserId + " 必须被拒绝");
            assertEquals(ResultCode.ACCESS_UNAUTHORIZED, ex.getResultCode(),
                    "userId=" + badUserId + " 应返回访问未授权");
        }

        verify(esiApiService, never()).getAccessTokenWithExpiry(any(), any());
    }

    @Test
    @DisplayName("characterId 为 null / 0 / 负数 -> 在查库前拒绝(FR-011)")
    void queryAccessToken_invalidCharacterId_rejectedBeforeQuery() throws Exception {
        for (Integer badCid : new Integer[]{null, 0, -5}) {
            EveHelperException ex = assertThrows(EveHelperException.class,
                    () -> characterApplicationService.queryAccessToken(badCid, USER_ID),
                    "characterId=" + badCid + " 必须被拒绝");
            assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
        }

        verify(esiApiService, never()).getAccessTokenWithExpiry(any(), any());
    }
}