package xyz.foolcat.eve.evehelper.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Leojan
 * date 2022-03-21 14:36
 */

@Schema(title="用户VO")
@Data
public class UserVO {
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
     * 用户邮箱
     */
    @Schema(name="用户邮箱")
    private String email;

}
