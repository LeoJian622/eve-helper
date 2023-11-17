package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 钱包记录
 *
 * @author Leojan
 * date 2023-11-16 21:49
 */

@Data
@Tag(name = "钱包记录 200 ok")
public class WalletJournalResponse {

    private Double amount;

    private Double balance;

    @JsonProperty("context_id")
    private Long contextId;

    @JsonProperty("context_id_type")
    private String contextIdType;

    private OffsetDateTime date;

    private String description;

    @JsonProperty("first_party_id")
    private Integer firstPartyId;

    private Long id;

    private String reason;

    @JsonProperty("ref_type")
    private String refType;

    @JsonProperty("second_party_id")
    private Integer secondPartyId;

    private Double tax;

    @JsonProperty("tax_receiver_id")
    private Integer taxReceiverId;
}
