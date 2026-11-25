package xyz.foolcat.eve.evehelper.application.dto.response;

/**
 * WebSocket 票据响应 DTO(022 US4)。
 *
 * @param ticket           UUID 票据
 * @param expiresInSeconds 有效期(秒)
 * @author Leojian
 */
public record WsTicketResponse(String ticket, Integer expiresInSeconds) {
}
