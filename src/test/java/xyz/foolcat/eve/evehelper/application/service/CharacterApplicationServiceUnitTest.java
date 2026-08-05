package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 角色应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色应用服务单元测试")
class CharacterApplicationServiceUnitTest {

    @Mock
    EsiGateway esiApiService;

    private CharacterApplicationService characterApplicationService;

    @BeforeEach
    void setUp() {
        characterApplicationService = new CharacterApplicationService(esiApiService);
    }

    @Test
    @DisplayName("授权成功 -> 调用 ESI getAccessToken")
    void authorizeCharacter_success() throws Exception {
        characterApplicationService.authorizeCharacter("char", "code123", 1);

        verify(esiApiService).getAccessToken("code123", 1);
    }

    @Test
    @DisplayName("ESI 抛 ParseException -> 转 EveHelperException")
    void authorizeCharacter_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(esiApiService).getAccessToken("code123", 1);

        assertThrows(EveHelperException.class,
                () -> characterApplicationService.authorizeCharacter("char", "code123", 1));
    }
}