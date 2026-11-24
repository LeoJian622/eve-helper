package xyz.foolcat.eve.evehelper.interfaces.web.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IMP-1 回归:EsiAccessException 工厂 → GlobalExceptionHandler HTTP 语义钉死。
 * <p>
 * 013 FR-005/FR-006 要求 ESI 数据权限不足映射 HTTP 403 + ESI00403(不折叠为笼统 400),
 * 授权失效/服务端错误以专属码可区分。工厂若丢失 IResultCode(resultCode=null),
 * 处理器会退化映射 400 —— 本测试直接以 MockHttpServletResponse 钉住该契约,
 * 不依赖 Spring 上下文/DB。
 *
 * @author Leojan
 * date 2026-11-24
 */
@DisplayName("IMP-1 EsiAccessException 工厂 HTTP 语义")
class GlobalExceptionHandlerEsiAccessTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("permissionLow() → HTTP 403 + ESI00403 + 友好文案")
    void permissionLow_maps403WithEsiCode() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Object> result = handler.handleEveHelperException(EsiAccessException.permissionLow(), response);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(result.getCode()).isEqualTo("ESI00403");
        assertThat(result.getMsg()).contains("Director");
    }

    @Test
    @DisplayName("authorizationFailure() → HTTP 400 + ESI00400(与 403 可区分)")
    void authorizationFailure_maps400WithEsiCode() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Object> result = handler.handleEveHelperException(EsiAccessException.authorizationFailure(), response);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(result.getCode()).isEqualTo("ESI00400");
        assertThat(result.getMsg()).isEqualTo(EsiAccessException.MSG_AUTHORIZATION_FAILURE);
    }

    @Test
    @DisplayName("serverFailure() → HTTP 400 + ESI00500(与 403 可区分)")
    void serverFailure_maps400WithEsiCode() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Object> result = handler.handleEveHelperException(EsiAccessException.serverFailure(), response);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(result.getCode()).isEqualTo("ESI00500");
        assertThat(result.getMsg()).isEqualTo(EsiAccessException.MSG_SERVER_FAILURE);
    }
}