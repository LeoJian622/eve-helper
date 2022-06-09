package xyz.foolcat.eve.evehelper.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 游戏角色表
    */
@ApiModel(value="游戏角色视图")
@Data
public class EveCharacterVO {

    @ApiModelProperty(value="")
    private Integer id;

    /**
     * 用户ID
     */
    @ApiModelProperty(value="用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @ApiModelProperty(value="角色ID")
    private Integer characterId;

    /**
     * 角色名
     */
    @ApiModelProperty(value="角色名")
    private String characterName;

    /**
     * 军团（公司）ID
     */
    @ApiModelProperty(value="军团（公司）ID")
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    @ApiModelProperty(value="军团（公司）名称")
    private String corpName;

    /**
     * 联盟ID
     */
    @ApiModelProperty(value="联盟ID")
    private Integer allianceId;

    /**
     * 联盟名称
     */
    @ApiModelProperty(value="联盟名称")
    private String allianceName;

}