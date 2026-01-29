package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 军团的联盟历史记录
 *
 * @author Leojan
 * date 2023-10-25 15:13
 */
@Data
@Tag(name = "军团的联盟历史记录 200 ok")
public class AllianceHistoryResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    @JsonProperty("record_id")
    private Integer recordId;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;
}
