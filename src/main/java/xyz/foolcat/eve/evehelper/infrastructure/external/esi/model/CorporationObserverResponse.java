package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.Date;

/**
 * 开采记录
 *
 * @author Leojan
 * date 2023-10-31 8:37
 */

@Data
@Tag(name = "开采记录 200 ok")
public class CorporationObserverResponse {

    @JsonProperty("last_updated")
    private Date lastUpdated;

    @JsonProperty("observer_id")
    private Long observerId;

    @JsonProperty("observer_type")
    private String observerType;
}
