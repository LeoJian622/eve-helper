package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
@TableName(value = "invuniquenames")
public class InvUniqueNamesPO implements Serializable {

    @TableId(value = "itemID")
    @Schema(description="")
    private Integer itemId;

    @TableField(value = "itemName")
    @Schema(description="")
    private String itemName;

    @TableField(value = "groupID")
    @Schema(description="")
    private Integer groupId;

    private static final long serialVersionUID = 1L;
} 