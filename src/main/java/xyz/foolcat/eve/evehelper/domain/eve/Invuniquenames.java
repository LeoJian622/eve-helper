package xyz.foolcat.eve.evehelper.domain.eve;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Schema
@Data
@TableName(value = "invuniquenames")
public class Invuniquenames implements Serializable {
    @TableId(value = "itemID", type = IdType.AUTO)
    @Schema(description="")
    private Integer itemid;

    @TableField(value = "itemName")
    @Schema(description="")
    private String itemname;

    @TableField(value = "groupID")
    @Schema(description="")
    private Integer groupid;

    private static final long serialVersionUID = 1L;
}