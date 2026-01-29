package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Item;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Position;

/**
 * @author Leojan
 * date 2023-09-28 14:36
 */

@Data
@Tag(name = "位标响应体 200 ok")
public class BookmarksResponse {

    @JsonProperty("bookmark_id")
    private Integer bookmarkId;

    @JsonProperty("coordinates")
    private Position coordinates;

    @JsonProperty("created")
    private String created;

    @JsonProperty("creator_id")
    private Integer creatorId;

    @JsonProperty("folder_id")
    private Integer folderId;

    @JsonProperty("item")
    private Item item;

    @JsonProperty("label")
    private String label;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("notes")
    private String notes;
}
