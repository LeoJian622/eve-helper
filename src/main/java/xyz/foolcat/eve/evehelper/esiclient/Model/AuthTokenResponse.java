package xyz.foolcat.eve.evehelper.esiclient.Model;

import lombok.Data;

/**
 * esi认证信息响应体
 *
 * @author Leojan
 * @date 2021-12-13 15:00
 */

@Data
public class AuthTokenResponse {

    private String access_token;

    private Integer expires_in;

    private String token_type;

    private String refresh_token;
}
