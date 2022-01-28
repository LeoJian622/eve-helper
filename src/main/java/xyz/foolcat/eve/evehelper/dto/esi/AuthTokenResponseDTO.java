package xyz.foolcat.eve.evehelper.dto.esi;

import lombok.Data;

/**
 * 用于接受esi返回的认证信息
 *
 * @author Leojan
 * @date 2021-12-13 15:00
 */

@Data
public class AuthTokenResponseDTO {

    private String access_token;

    private Integer expires_in;

    private String token_type;

    private String refresh_token;
}
