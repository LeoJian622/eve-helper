package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发送邮件请求 DTO(022 US2)。
 * <p>
 * 校验规则(data-model §6 逐字):
 * recipients @NotEmpty @Size(max=50), 元素 recipientId @NotNull + recipientType 枚举;
 * subject @NotBlank @Size(max=128); body @NotBlank @Size(max=10000)。
 *
 * @author Leojan
 */
public record SendMailRequest(
        @NotEmpty @Size(max = 50)
        @Valid
        List<Recipient> recipients,

        @NotBlank @Size(max = 128)
        String subject,

        @NotBlank @Size(max = 10000)
        String body
) {

    /**
     * 收件人条目。
     *
     * @param recipientId   收件人 ID(@NotNull)
     * @param recipientType 收件人类型枚举(character/corporation/alliance/mailing_list)
     */
    public record Recipient(
            @NotNull
            Integer recipientId,

            @Pattern(regexp = "character|corporation|alliance|mailing_list")
            String recipientType
    ) {
    }
}
