package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 角色雇佣记录
 *
 * @author Leojan
 * date 2023-10-19 15:57
 */

@Data
@Tag(name = "角色雇佣记录响应体 200 ok")
public class CorporationHistoryResponse {

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    @JsonProperty("record_id")
    private Integer recordId;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;

}
