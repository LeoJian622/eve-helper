package xyz.foolcat.eve.evehelper.esiclient.Model;

import lombok.Data;

/**
 * 错误响应体
 *
 * @author Leojan
 * @date 2023-08-02 11:52
 */

@Data
public class AuthErrorResponse {

    private String error;

    private Integer timeout;

    private String error_description;
}
