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
    * 用户和角色关联表
    */
@ApiModel(value="用户和角色关联表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_user_role")
public class SysUserRole extends BaseEntity {
    /**
     * 用户ID
     */
    @ApiModelProperty(value="用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @ApiModelProperty(value="角色ID")
    private Integer roleId;

    public static final String COL_USER_ID = "user_id";

    public static final String COL_ROLE_ID = "role_id";
}