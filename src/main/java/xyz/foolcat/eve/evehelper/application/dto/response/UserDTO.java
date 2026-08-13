package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Leojan
 * date 2022-03-16 17:13
 */

@Schema(description = "用户注册DTO")
@Data
public class UserDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 性别：1-男 2-女
     */
    @Schema(description = "性别：1-男 2-女")
    private Boolean gender;

    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 用户邮箱
     */
    @Schema(description = "用户邮箱")
    private String email;
}
