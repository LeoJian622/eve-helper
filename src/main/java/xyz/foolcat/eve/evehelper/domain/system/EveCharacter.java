package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 游戏角色表
 */
@Schema(description = "游戏角色表")
@Data
@TableName(value = "eve_character")
public class EveCharacter implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Integer id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @TableField(value = "character_id")
    @Schema(description = "角色ID")
    private Integer characterId;

    /**
     * 角色名
     */
    @TableField(value = "character_name")
    @Schema(description = "角色名")
    private String characterName;

    /**
     * 军团（公司）ID
     */
    @TableField(value = "corp_id")
    @Schema(description = "军团（公司）ID")
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    @TableField(value = "corp_name")
    @Schema(description = "军团（公司）名称")
    private String corpName;

    /**
     * 联盟ID
     */
    @TableField(value = "alliance_id")
    @Schema(description = "联盟ID")
    private Integer allianceId;

    /**
     * 联盟名称
     */
    @TableField(value = "alliance_name")
    @Schema(description = "联盟名称")
    private String allianceName;

    /**
     * 角色授权
     */
    @TableField(value = "refresh_token")
    @Schema(description = "角色授权")
    private String refreshToken;

    /**
     * 个人授权
     */
    @TableField(value = "refresh_token_char")
    @Schema(description = "个人授权")
    private String refreshTokenChar;

    /**
     * 军团授权
     */
    @TableField(value = "refresh_token_crop")
    @Schema(description = "军团授权")
    private String refreshTokenCrop;

    @TableField(value = "gmt_create")
    @Schema(description = "")
    private Date gmtCreate;

    @TableField(value = "gmt_modified")
    @Schema(description = "")
    private Date gmtModified;

    /**
     * CH:0 EU:1
     */
    @TableField(value = "`type`")
    @Schema(description = "CH:0 EU:1")
    private Integer type;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_USER_ID = "user_id";

    public static final String COL_CHARACTER_ID = "character_id";

    public static final String COL_CHARACTER_NAME = "character_name";

    public static final String COL_CORP_ID = "corp_id";

    public static final String COL_CORP_NAME = "corp_name";

    public static final String COL_ALLIANCE_ID = "alliance_id";

    public static final String COL_ALLIANCE_NAME = "alliance_name";

    public static final String COL_REFRESH_TOKEN = "refresh_token";

    public static final String COL_REFRESH_TOKEN_CHAR = "refresh_token_char";

    public static final String COL_REFRESH_TOKEN_CROP = "refresh_token_crop";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";

    public static final String COL_TYPE = "type";
}