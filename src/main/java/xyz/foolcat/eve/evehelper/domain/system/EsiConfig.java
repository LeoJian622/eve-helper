package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ESI配置表
 */
@Schema(description = "ESI配置表")
@Data
@TableName(value = "esi_config")
public class EsiConfig implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Integer id;

    /**
     * ESI请求地址
     */
    @TableField(value = "base_url")
    @Schema(description = "ESI请求地址")
    private String baseUrl;

    /**
     * ESI版本
     */
    @TableField(value = "version")
    @Schema(description = "ESI版本")
    private String version;

    /**
     * CH:0 EU:1
     */
    @TableField(value = "`type`")
    @Schema(description = "CH:0 EU:1")
    private Integer type;

    @TableField(value = "gmt_create")
    @Schema(description = "")
    private Date gmtCreate;

    @TableField(value = "gmt_modified")
    @Schema(description = "")
    private Date gmtModified;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_BASE_URL = "base_url";

    public static final String COL_VERSION = "version";

    public static final String COL_TYPE = "type";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}