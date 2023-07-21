package xyz.foolcat.eve.evehelper.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
    * 游戏角色表
    */
@Schema(title="游戏角色视图")
@Data
public class EveCharacterVO {

    @Schema(name="")
    private Integer id;

    /**
     * 用户ID
     */
    @Schema(name="用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @Schema(name="角色ID")
    private Integer characterId;

    /**
     * 角色名
     */
    @Schema(name="角色名")
    private String characterName;

    /**
     * 军团（公司）ID
     */
    @Schema(name="军团（公司）ID")
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    @Schema(name="军团（公司）名称")
    private String corpName;

    /**
     * 联盟ID
     */
    @Schema(name="联盟ID")
    private Integer allianceId;

    /**
     * 联盟名称
     */
    @Schema(name="联盟名称")
    private String allianceName;

}