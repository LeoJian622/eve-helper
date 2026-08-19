package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EsiStatusUtil 单元测试(TDD 红-绿)。
 *
 * <p>覆盖 403 单识语义(H1/H2/M1 设计决策):</p>
 * <ul>
 *     <li>isForbidden(403)=true,其余状态=false</li>
 *     <li>forbiddenAgent 返回 EsiException(ESI_AUTH_PERMISSION_LOW),message 用枚举友好文案,<b>不含 ESI 原始 error/errorDescription</b>(安全红线)</li>
 * </ul>
 */
@DisplayName("EsiStatusUtil ESI 状态识别")
class EsiStatusUtilTest {

    @Test
    @DisplayName("isForbidden:403 为 true,非 403 为 false")
    void isForbidden_403_returnsTrue() {
        assertThat(EsiStatusUtil.isForbidden(HttpStatus.FORBIDDEN)).isTrue();
        assertThat(EsiStatusUtil.isForbidden(HttpStatus.UNAUTHORIZED)).isFalse();
        assertThat(EsiStatusUtil.isForbidden(HttpStatus.BAD_REQUEST)).isFalse();
        assertThat(EsiStatusUtil.isForbidden(HttpStatus.INTERNAL_SERVER_ERROR)).isFalse();
    }

    @Test
    @DisplayName("forbiddenAgent:返回 ESI_AUTH_PERMISSION_LOW,message 用友好文案不含原始错误串")
    void forbiddenAgent_messageUsesFriendlyCopy_notRawError() {
        EsiException e = EsiStatusUtil.forbiddenAgent();

        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW);
        // H2:message 必须是枚举友好文案,绝不含 ESI 原始 error/errorDescription
        String msg = e.getMessage();
        assertThat(msg)
                .contains("缺少目标军团的")
                .doesNotContain(":")          // ESI 原始串形如 "Forbidden:..." 含冒号
                .satisfies(m -> assertThat(m).isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW.getMsg()));
    }

    @Test
    @DisplayName("dataError:403 → ESI_AUTH_PERMISSION_LOW(M1:先判状态,不被 body 劫持)")
    void dataError_403_mapsPermissionLow() {
        ClientResponse resp = ClientResponse.create(HttpStatus.FORBIDDEN).build();

        assertThatThrownBy(() -> EsiStatusUtil.dataError().apply(resp).block())
                .isInstanceOfSatisfying(EsiException.class, e -> {
                    assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW);
                    // H2:message 友好,不含 ESI 原始串
                    assertThat(e.getMessage()).doesNotContain("Forbidden");
                });
    }

    @Test
    @DisplayName("dataError:其余 4xx(401) → ESI_AUTHORIZATION_FAILURE")
    void dataError_other4xx_mapsAuthorizationFailure() {
        ClientResponse resp = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();

        assertThatThrownBy(() -> EsiStatusUtil.dataError().apply(resp).block())
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }

    @Test
    @DisplayName("dataError:5xx(500) → ESI_SERVER_FAILURE")
    void dataError_5xx_mapsServerFailure() {
        ClientResponse resp = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();

        assertThatThrownBy(() -> EsiStatusUtil.dataError().apply(resp).block())
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_SERVER_FAILURE));
    }
}