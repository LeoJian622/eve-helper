package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Leojan
 * @date 2022-03-16 17:13
 */

@ApiModel(value="用户注册DTO")
@Data
public class UserDTO {

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
     * 密码
     */
    @ApiModelProperty(value="密码")
    private String password;

    /**
     * 用户邮箱
     */
    @ApiModelProperty(value="用户邮箱")
    private String email;

    public static final String COL_USERNAME = "username";

    public static final String COL_NICKNAME = "nickname";

    public static final String COL_GENDER = "gender";

    public static final String COL_PASSWORD = "password";

    public static final String COL_EMAIL = "email";
}
