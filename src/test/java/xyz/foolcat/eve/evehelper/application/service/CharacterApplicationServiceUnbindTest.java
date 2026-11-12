package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 解绑角色应用服务单元测试（T042，2 用例）。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("解绑角色应用服务测试(T042)")
class CharacterApplicationServiceUnbindTest {

    @Mock private EsiGateway esiGateway;
    @Mock private AuthorizeUtil authorizeUtil;
    @Mock private EveAccountRepository eveAccountRepository;
    @Mock private CacheGateway cacheGateway;

    private CharacterApplicationService service;

    private static final Integer USER_ID = 42;
    private static final Integer CHARACTER_ID = 1001;

    private EveAccount account;

    @BeforeEach
    void setUp() {
        service = new CharacterApplicationService(esiGateway, authorizeUtil, eveAccountRepository, cacheGateway);

        account = new EveAccount();
        account.setUserId(USER_ID);
        account.setCharacterId(CHARACTER_ID);
        account.setCorpId(500);
    }

    // ── AC: 解绑后清理 accessToken 缓存 ──

    @Test
    @DisplayName("unbind_deletesAccessTokenCache: 删除绑定 + 清理 esi_access_token 缓存")
    void unbind_deletesAccessTokenCache() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(eveAccountRepository.deleteByUserIdAndCharacterId(USER_ID, CHARACTER_ID)).thenReturn(1);

            service.unbindCharacter(CHARACTER_ID);

            verify(eveAccountRepository).deleteByUserIdAndCharacterId(USER_ID, CHARACTER_ID);
            verify(cacheGateway).delete("esi_access_token:42:1001");
        }
    }

    // ── AC: 解绑后再访问 fail-closed（016 先例） ──

    @Test
    @DisplayName("unbind_subsequentCharacterEndpoint_failsClosed: 解绑后 queryAccessToken 被拒")
    void unbind_subsequentCharacterEndpoint_failsClosed() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(eveAccountRepository.deleteByUserIdAndCharacterId(USER_ID, CHARACTER_ID)).thenReturn(1);

            service.unbindCharacter(CHARACTER_ID);

            // 解绑后,绑定行已删,esiGateway.getAccessTokenWithExpiry 模拟 ACCESS_UNAUTHORIZED
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID))
                    .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

            assertThatThrownBy(() -> service.queryAccessToken(CHARACTER_ID, USER_ID))
                    .isInstanceOf(EveHelperException.class)
                    .satisfies(e -> assertThat(((EveHelperException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));
        }
    }
}
