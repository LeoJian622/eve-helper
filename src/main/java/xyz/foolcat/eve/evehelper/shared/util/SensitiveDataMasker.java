package xyz.foolcat.eve.evehelper.shared.util;

/**
 * 敏感数据脱敏工具类
 * 用于日志记录时隐藏敏感信息
 *
 * @author Leojan
 * date 2026-02-01
 */
public class SensitiveDataMasker {

    private static final String MASK = "****";
    private static final int VISIBLE_LENGTH = 4;

    /**
     * 脱敏Token(显示前4位和后4位)
     *
     * @param token 原始token
     * @return 脱敏后的token
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "***";
        }

        if (token.length() < 8) {
            return "***";
        }

        String prefix = token.substring(0, VISIBLE_LENGTH);
        String suffix = token.substring(token.length() - VISIBLE_LENGTH);
        return prefix + MASK + suffix;
    }

    /**
     * 脱敏用户名(显示前2位和后1位)
     *
     * @param username 原始用户名
     * @return 脱敏后的用户名
     */
    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "***";
        }

        if (username.length() < 3) {
            return "***";
        }

        String prefix = username.substring(0, 2);
        String suffix = username.substring(username.length() - 1);
        return prefix + MASK + suffix;
    }

    /**
     * 脱敏邮箱(保留首字母和域名)
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String prefix = email.substring(0, 1);
        String domain = email.substring(atIndex);
        return prefix + "***" + domain;
    }
}
