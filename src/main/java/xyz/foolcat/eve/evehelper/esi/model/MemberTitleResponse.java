package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 成员职位信息
 *
 * @author Leojan
 * @date 2023-10-26 9:44
 */

@Data
@Tag(name = "成员职位信息响应体 200 ok")
public class MemberTitleResponse {

    @JsonProperty("character_id")
    private Integer characterId;

    private List<Integer> titles;

}
