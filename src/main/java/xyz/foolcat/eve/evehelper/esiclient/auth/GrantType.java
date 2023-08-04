package xyz.foolcat.eve.evehelper.esiclient.auth;

/**
 * @author Leojan
 * @date 2023-08-02 9:40
 */

public enum GrantType {

    /**
     * 认证方式
     */
    AUTHORIZATION_CODE("authorization_code"),
    REFRESH_TOKEN("refresh_token"),
    ;

    /**
     * 授权方式举例
     */
    private String grantType;

    GrantType(String grantType) {
        this.grantType = grantType;
    }

    @Override
    public String toString() {
        return this.grantType;
    }

}
