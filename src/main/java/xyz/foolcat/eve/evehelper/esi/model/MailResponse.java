package xyz.foolcat.eve.evehelper.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Recipient;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 邮件信息
 *
 * @author Leojan
 * @date 2023-10-31 16:56
 */

@Data
@Tag(name = "邮件信息 200 ok")
public class MailResponse {

    private String body;

    private Integer from;

    private List<Integer> labels;

    private Boolean read;

    private List<Recipient> recipients;

    private String subject;

    private OffsetDateTime timestamp;
}
