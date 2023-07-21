package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Leojan
 * @date 2022-03-16 17:13
 */

@Schema(title="用户注册DTO")
@Data
public class UserDTO {

    /**
     * 用户名
     */
    @Schema(name="用户名")
    private String username;

    /**
     * 昵称
     */
    @Schema(name="昵称")
    private String nickname;

    /**
     * 性别：1-男 2-女
     */
    @Schema(name="性别：1-男 2-女")
    private Boolean gender;

    /**
     * 密码
     */
    @Schema(name="密码")
    private String password;

    /**
     * 用户邮箱
     */
    @Schema(name="用户邮箱")
    private String email;

    public static final String COL_USERNAME = "username";

    public static final String COL_NICKNAME = "nickname";

    public static final String COL_GENDER = "gender";

    public static final String COL_PASSWORD = "password";

    public static final String COL_EMAIL = "email";
}
