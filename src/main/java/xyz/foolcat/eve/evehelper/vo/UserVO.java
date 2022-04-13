package xyz.foolcat.eve.evehelper.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Leojan
 * @date 2022-03-21 14:36
 */

@ApiModel(value="用户VO")
@Data
public class UserVO {
    /**
     * 用户名
     */
    @ApiModelProperty(value="用户名")
    private String username;

    /**
     * 昵称
     */
    @ApiModelProperty(value="昵称")
    private String nickname;

    /**
     * 性别：1-男 2-女
     */
    @ApiModelProperty(value="性别：1-男 2-女")
    private Boolean gender;


    /**
     * 用户邮箱
     */
    @ApiModelProperty(value="用户邮箱")
    private String email;

}
