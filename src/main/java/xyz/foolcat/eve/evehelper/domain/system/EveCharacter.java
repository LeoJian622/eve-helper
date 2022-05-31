package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 游戏角色表
    */
@ApiModel(value="游戏角色表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "eve_character")
public class EveCharacter extends BaseEntity{
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value="")
    private Integer id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @ApiModelProperty(value="用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @TableField(value = "character_id")
    @ApiModelProperty(value="角色ID")
    private Integer characterId;

    /**
     * 角色名
     */
    @TableField(value = "character_name")
    @ApiModelProperty(value="角色名")
    private String characterName;

    /**
     * 军团（公司）ID
     */
    @TableField(value = "corp_id")
    @ApiModelProperty(value="军团（公司）ID")
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    @TableField(value = "corp_name")
    @ApiModelProperty(value="军团（公司）名称")
    private String corpName;

    /**
     * 联盟ID
     */
    @TableField(value = "alliance_id")
    @ApiModelProperty(value="联盟ID")
    private Integer allianceId;

    /**
     * 联盟名称
     */
    @TableField(value = "alliance_name")
    @ApiModelProperty(value="联盟名称")
    private String allianceName;

    @TableField(value = "roles")
    @ApiModelProperty(value="")
    private String roles;

    /**
     * 人物授权
     */
    @TableField(value = "refresh_token_char")
    @ApiModelProperty(value="人物授权")
    private String refreshTokenChar;

    /**
     * 军团授权
     */
    @TableField(value = "refresh_token_crop")
    @ApiModelProperty(value="军团授权")
    private String refreshTokenCrop;


    @TableField(value = "refresh_token_skill")
    @ApiModelProperty(value="技能授权")
    private String refreshTokenSkill;


    @TableField(value = "refresh_token_normal")
    @ApiModelProperty(value="通用授权")
    private String refreshTokenNormal;

    /**
     * CH:0 EU:1
     */
    @TableField(value = "`type`")
    @ApiModelProperty(value="CH:0 EU:1")
    private Integer type;

    public static final String COL_ID = "id";

    public static final String COL_USER_ID = "user_id";

    public static final String COL_CHARACTER_ID = "character_id";

    public static final String COL_CHARACTER_NAME = "character_name";

    public static final String COL_CORP_ID = "corp_id";

    public static final String COL_CORP_NAME = "corp_name";

    public static final String COL_ALLIANCE_ID = "alliance_id";

    public static final String COL_ALLIANCE_NAME = "alliance_name";

    public static final String COL_ROLES = "roles";

    public static final String COL_REFRESH_TOKEN_CHAR = "refresh_token_char";

    public static final String COL_REFRESH_TOKEN_CROP = "refresh_token_crop";

    public static final String COL_REFRESH_TOKEN_SKILL = "refresh_token_skill";

    public static final String COL_TYPE = "type";
}