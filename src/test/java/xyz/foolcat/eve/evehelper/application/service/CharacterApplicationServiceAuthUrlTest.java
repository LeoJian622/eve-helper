package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.dto.response.AuthorizeUrlResponse;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 授权 URL 应用服务单元测试（T043，2 用例）。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("授权 URL 应用服务测试(T043)")
class CharacterApplicationServiceAuthUrlTest {

    @Mock private EsiGateway esiGateway;
    @Mock private AuthorizeUtil authorizeUtil;
    @Mock private EveAccountRepository eveAccountRepository;

    private CharacterApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CharacterApplicationService(esiGateway, authorizeUtil, eveAccountRepository);
    }

    // ── AC: 委托端口并返回 state ──

    @Test
    @DisplayName("authUrl_delegatesToPortAndReturnsState: URL 与 state 正确")
    void authUrl_delegatesToPortAndReturnsState() {
        String expectedUrl = "https://login.test/authorize?x=1";
        when(esiGateway.buildAuthorizeUrl("serenity")).thenReturn(expectedUrl);

        AuthorizeUrlResponse response = service.getAuthorizeUrl("serenity");

        assertThat(response.authorizeUrl()).isEqualTo(expectedUrl);
        assertThat(response.state()).isEqualTo("eve-helper");
        verify(esiGateway).buildAuthorizeUrl("serenity");
    }

    // ── AC: 端口拒绝传播 ──

    @Test
    @DisplayName("authUrl_portRejection_propagates: PARAM_ERROR 传播")
    void authUrl_portRejection_propagates() {
        when(esiGateway.buildAuthorizeUrl("tranquility"))
                .thenThrow(new EveHelperException(ResultCode.PARAM_ERROR));

        assertThatThrownBy(() -> service.getAuthorizeUrl("tranquility"))
                .isInstanceOf(EveHelperException.class)
                .satisfies(e -> assertThat(((EveHelperException) e).getResultCode())
                        .isEqualTo(ResultCode.PARAM_ERROR));
    }
}
