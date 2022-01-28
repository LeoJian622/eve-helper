package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 角色表
    */
@ApiModel(value="角色表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_role")
public class SysRole extends BaseEntity  {
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value="")
    private Long id;

    /**
     * 角色名称
     */
    @TableField(value = "`name`")
    @ApiModelProperty(value="角色名称")
    private String name;

    /**
     * 角色编码
     */
    @TableField(value = "code")
    @ApiModelProperty(value="角色编码")
    private String code;

    /**
     * 显示顺序
     */
    @TableField(value = "sort")
    @ApiModelProperty(value="显示顺序")
    private Integer sort;

    /**
     * 角色状态：0-正常；1-停用
     */
    @TableField(value = "`status`")
    @ApiModelProperty(value="角色状态：0-正常；1-停用")
    private Boolean status;

    /**
     * 逻辑删除标识：0-未删除；1-已删除
     */
    @TableField(value = "deleted")
    @ApiModelProperty(value="逻辑删除标识：0-未删除；1-已删除")
    private Boolean deleted;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_CODE = "code";

    public static final String COL_SORT = "sort";

    public static final String COL_STATUS = "status";

    public static final String COL_DELETED = "deleted";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}