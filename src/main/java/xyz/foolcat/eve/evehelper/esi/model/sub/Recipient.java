package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 收件人
 *
 * @author Leojan
 * date 2023-10-31 16:54
 */

@Data
@Tag(name = "收件人")
public class Recipient {

    @JsonProperty("recipient_id")
    private Integer recipientId;

    /**
     * alliance, character, corporation, mailing_list
     */
    @JsonProperty("recipient_type")
    private String recipientType;
}
