package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * @author Leojan
 * @date 2023-09-28 15:05
 */

@Data
@Tag(name = "位标文件夹响应体 200 ok")
public class BookmarkFoldersResponse {

    @JsonProperty("creator_id")
    private Integer creatorId;

    @JsonProperty("folder_id")
    private Integer folderId;

    @JsonProperty("name")
    private String name;
}
