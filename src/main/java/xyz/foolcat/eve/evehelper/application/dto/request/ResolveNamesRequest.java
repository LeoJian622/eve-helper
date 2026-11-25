package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 名称解析请求 DTO(022 US2)。
 * <p>
 * 校验规则(data-model §6 逐字):
 * names @NotEmpty @Size(max=50), 元素 @NotBlank @Size(max=128)。
 *
 * @author Leojan
 */
public record ResolveNamesRequest(
        @NotEmpty @Size(max = 50)
        @Valid
        List<@NotBlank @Size(max = 128) String> names
) {
}
