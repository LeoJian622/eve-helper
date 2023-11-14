package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 军团股东信息
 *
 * @author Leojan
 * date 2023-10-26 11:22
 */

@Data
@Tag(name = "军团股东信息 200 ok")
public class ShareHolderResponse {

    @JsonProperty("share_count")
    private Long shareCount;

    @JsonProperty("shareholder_id")
    private Integer shareholderId;

    @JsonProperty("shareholder_type")
    private String shareholderType;
}
