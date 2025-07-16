package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 击毁报告ID和HASH值
 *
 * @author Leojan
 * date 2023-10-31 11:59
 */

@Data
@Tag(name = "击毁报告ID和HASH值 200 ok")
public class KillMailsIdAndHashResponse {

    @JsonProperty("killmail_hash")
    private String killMailHash;

    @JsonProperty("killmail_id")
    private Integer killMailId;
}
