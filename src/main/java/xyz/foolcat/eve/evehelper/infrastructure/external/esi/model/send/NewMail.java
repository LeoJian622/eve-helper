package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient;

import java.util.List;

/**
 * 邮件详情
 *
 * @author Leojan
 * date 2023-11-01 14:41
 */

@Data
@Tag(name = "新邮件")
public class NewMail {

    @JsonProperty("approved_cost")
    private Long approvedCost = 0L;

    private String body;

    private List<Recipient> recipients;

    private String subject;

}
