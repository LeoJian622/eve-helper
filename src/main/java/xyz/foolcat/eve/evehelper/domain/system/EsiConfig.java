package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * ESI配置表
    */


@Schema(title="ESI配置表")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "esi_config")
public class EsiConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name="")
    private Integer id;

    /**
     * ESI请求地址
     */
    @TableField(value = "base_url")
    @Schema(name="ESI请求地址")
    private String baseUrl;

    /**
     * ESI版本
     */
    @TableField(value = "version")
    @Schema(name="ESI版本")
    private String version;

    /**
     * CH:0 EU:1
     */
    @TableField(value = "`type`")
    @Schema(name="CH:0 EU:1")
    private Integer type;

    public static final String COL_ID = "id";

    public static final String COL_BASE_URL = "base_url";

    public static final String COL_VERSION = "version";

    public static final String COL_TYPE = "type";
}