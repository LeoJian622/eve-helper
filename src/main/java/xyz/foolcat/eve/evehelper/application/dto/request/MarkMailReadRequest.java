package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 标记邮件已读/未读请求 DTO(022 US2)。
 * <p>
 * 校验规则(data-model §6 逐字): read @NotNull(Boolean)。
 *
 * @author Leojan
 */
public record MarkMailReadRequest(
        @NotNull
        Boolean read
) {
}
