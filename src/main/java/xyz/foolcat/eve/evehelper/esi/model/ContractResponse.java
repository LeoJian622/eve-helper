package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 合同详情
 *
 * @author Leojan
 * @date 2023-10-24 16:42
 */

@Data
@Tag(name = "合同详情响应体 200 ok")
public class ContractResponse {

    @JsonProperty("acceptor_id")
    private Integer acceptorId;

    @JsonProperty("assignee_id")
    private Integer assigneeId;

    private String availability;

    private Double buyout;

    private Double collateral;

    @JsonProperty("contract_id")
    private Integer contractId;

    @JsonProperty("date_accepted")
    private OffsetDateTime dateAccepted;

    @JsonProperty("date_completed")
    private OffsetDateTime dateCompleted;

    @JsonProperty("date_expired")
    private OffsetDateTime dateExpired;

    @JsonProperty("date_issued")
    private OffsetDateTime dateIssued;

    @JsonProperty("days_to_complete")
    private Integer daysToComplete;

    @JsonProperty("end_location_id")
    private Long endLocationId;

    @JsonProperty("for_corporation")
    private Boolean forCorporation;

    @JsonProperty("issuer_corporation_id")
    private Integer issuerCorporationId;

    @JsonProperty("issuer_id")
    private Integer issuerId;

    private Double price;

    private Double reward;

    @JsonProperty("start_location_id")
    private Long startLocationId;

    private String status;

    private String title;

    private String type;

    private Double volume;
}
