package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(title="observer",hidden = true)
@Data
@TableName(value = "observer")
public class Observer implements Serializable {
    @TableId(value = "observer_id", type = IdType.AUTO)
    @Schema(name="")
    private Long observerId;

    /**
     * 观察者类型  枚举：structure 
     */
    @TableField(value = "observer_type")
    @Schema(name="观察者类型  枚举：structure ")
    private String observerType;

    /**
     * 上次更新时间
     */
    @TableField(value = "last_updated")
    @Schema(name="上次更新时间")
    private Date lastUpdated;

    @TableField(value = "croporation_id")
    @Schema(name="")
    private Long croporationId;

    private static final long serialVersionUID = 1L;

    public static final String COL_OBSERVER_ID = "observer_id";

    public static final String COL_OBSERVER_TYPE = "observer_type";

    public static final String COL_LAST_UPDATED = "last_updated";

    public static final String COL_CROPORATION_ID = "croporation_id";
}