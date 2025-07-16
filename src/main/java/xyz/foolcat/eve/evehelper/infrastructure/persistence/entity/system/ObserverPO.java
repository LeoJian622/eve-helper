package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * 观察者表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "观察者表")
@Data
@TableName(value = "observer")
public class ObserverPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField(value = "observer_id")
    @Schema(description = "观察者ID")
    private Long observerId;

    /**
     * 观察者类型  枚举：structure
     */
    @TableField(value = "observer_type")
    @Schema(description = "观察者类型  枚举：structure ")
    private String observerType;

    /**
     * 上次更新时间
     */
    @TableField(value = "last_updated")
    @Schema(description = "上次更新时间")
    private Date lastUpdated;

    @TableField(value = "corporation_id")
    @Schema(description = "公司ID")
    private Long corporationId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_OBSERVER_ID = "observer_id";

    public static final String COL_OBSERVER_TYPE = "observer_type";

    public static final String COL_LAST_UPDATED = "last_updated";

    public static final String COL_CORPORATION_ID = "corporation_id";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 