package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 邮件基本信息
 *
 * @author Leojan
 * date 2023-10-31 16:56
 */

@Data
@Tag(name = "邮件基本信息 200 ok")
public class RequestedMailResponse {

    private Integer from;

    @JsonProperty("is_read")
    private Boolean isRead;

    private List<Integer> labels;

    @JsonProperty("mail_id")
    private Integer mailId;

    private List<Recipient> recipients;

    private String subject;

    private OffsetDateTime timestamp;
}
