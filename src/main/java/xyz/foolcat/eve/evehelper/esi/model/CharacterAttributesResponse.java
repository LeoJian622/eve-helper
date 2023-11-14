package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 人物属性
 *
 * @author Leojan
 * date 2023-11-14 11:04
 */
@Data
@Tag(name = "人物属性响应体 200 ok")
public class CharacterAttributesResponse {

    @JsonProperty("accrued_remap_cooldown_date")
    private OffsetDateTime accruedRemapCooldownDate;

    @JsonProperty("bonus_remaps")
    private Integer bonusRemaps;

    private Integer charisma;

    private Integer intelligence;

    @JsonProperty("last_remap_date")
    private OffsetDateTime lastRemapDate;

    private Integer memory;

    private Integer perception;

    private Integer willpower;

}
