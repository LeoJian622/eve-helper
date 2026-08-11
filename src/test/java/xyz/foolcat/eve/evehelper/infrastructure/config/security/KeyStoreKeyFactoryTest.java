package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link KeyStoreKeyFactory} 契约测试(007 FR-023,T020;M-2/M-4 修复合约)。
 *
 * <p>固化重写后的目标行为(当前旧实现不满足 → RED):</p>
 * <ul>
 *   <li><b>异常分类</b>:别名不存在 → 消息含「别名」;口令错 → 消息<b>不含口令原文</b>
 *       且指明口令问题;文件不存在 → 消息含明确文件名(旧实现统一 "Cannot load keys")</li>
 *   <li><b>RSA 位数</b>:modulus &lt; 2048 拒绝加载(旧实现不校验)</li>
 *   <li><b>双物理格式</b>:PKCS12 与 JKS fixture 均可加载(M-4:现 test-only.jks
 *       物理上是 PKCS12,靠 JDK DualFormat 被 {@code getInstance("jks")} 加载,
 *       重写不得破坏)</li>
 *   <li><b>资源安全</b>:加载后 InputStream 必须关闭(旧实现泄漏,MEDIUM-2)</li>
 * </ul>
 *
 * <p>fixture 为 keytool 预生成的<b>合成测试密钥</b>(src/test/resources/keystore-fixtures/,
 * 口令 synthetic-store-pass / synthetic-key-pass 公开于本文件),不含生产密钥材料:</p>
 * <ul>
 *   <li>{@code rsa2048.p12} / {@code rsa2048.jks}:别名 {@code t020},2048 位</li>
 *   <li>{@code rsa1024.p12}:别名 {@code t020-weak},1024 位(弱密钥门禁用例)</li>
 * </ul>
 *
 * <p>TDD 状态:T020 = RED(对旧实现的断言失败);T022 重写后转 GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("KeyStoreKeyFactory 契约测试(007 FR-023)")
class KeyStoreKeyFactoryTest {

    /** 合成测试口令(与 fixture 生成时一致,非任何真实凭证) */
    private static final char[] STORE_PASSWORD = "synthetic-store-pass".toCharArray();
    /** 仅 JKS fixture 有独立 keypass;PKCS12 为单口令格式,keypass 物理上等于 storepass */
    private static final char[] KEY_PASSWORD = "synthetic-key-pass".toCharArray();
    private static final String ALIAS = "t020";
    private static final String WEAK_ALIAS = "t020-weak";

    private static final Path FIXTURE_DIR = Path.of("src", "test", "resources", "keystore-fixtures");

    private static Path fixture(String name) {
        Path path = FIXTURE_DIR.resolve(name);
        assertTrue(Files.exists(path), "fixture 缺失: " + path.toAbsolutePath());
        return path;
    }

    // ------------------------------------------------------------------
    // 双物理格式加载(M-4)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PKCS12 物理格式 fixture 可加载(现状兼容,不得破坏)")
    void load_pkcs12PhysicalFormat_success() {
        // Arrange + Act(PKCS12 单口令:keypass 用 storepass)
        KeyPair keyPair = new KeyStoreKeyFactory(
                new FileSystemResource(fixture("rsa2048.p12").toFile()), STORE_PASSWORD)
                .getKeyPair(ALIAS, STORE_PASSWORD);

        // Assert
        assertNotNull(keyPair, "PKCS12 fixture 应能加载");
        assertTrue(keyPair.getPublic() instanceof RSAPublicKey, "应为 RSA 密钥对");
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        assertTrue(publicKey.getModulus().bitLength() >= 2048, "应为 2048 位密钥");
    }

    @Test
    @DisplayName("JKS 物理格式 fixture 可加载(双格式支持,魔数已验证为 FEEDFEED)")
    void load_jksPhysicalFormat_success() {
        // Arrange + Act(rsa2048.jks 由 keytool -storetype JKS 生成,物理格式为 JKS)
        KeyPair keyPair = new KeyStoreKeyFactory(
                new FileSystemResource(fixture("rsa2048.jks").toFile()), STORE_PASSWORD)
                .getKeyPair(ALIAS, KEY_PASSWORD);

        // Assert
        assertNotNull(keyPair, "JKS fixture 应能加载");
        assertTrue(keyPair.getPublic() instanceof RSAPublicKey, "应为 RSA 密钥对");
    }

    // ------------------------------------------------------------------
    // 异常分类(消息可排障,不泄密)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("别名不存在 → 错误信息含「别名」与所请求别名")
    void load_aliasNotFound_messageMentionsAlias() {
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new KeyStoreKeyFactory(
                        new FileSystemResource(fixture("rsa2048.p12").toFile()), STORE_PASSWORD)
                        .getKeyPair("no-such-alias", STORE_PASSWORD));
        assertTrue(ex.getMessage().contains("别名"),
                "错误信息应指明「别名」问题,实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("no-such-alias"),
                "错误信息应含所请求的别名,实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("口令错误 → 错误信息提及口令问题但不含口令原文")
    void load_wrongPassword_messageDoesNotLeakPassword() {
        // Arrange
        String wrongPassword = "wrong-PASSW0RD-super-secret";

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new KeyStoreKeyFactory(
                        new FileSystemResource(fixture("rsa2048.p12").toFile()), wrongPassword.toCharArray())
                        .getKeyPair(ALIAS, STORE_PASSWORD));
        assertFalse(ex.getMessage().contains(wrongPassword),
                "错误信息绝不得回显口令原文");
        assertFalse(fullChainMessages(ex).contains(wrongPassword),
                "完整异常链也不得含口令原文");
        assertTrue(ex.getMessage().contains("口令"),
                "错误信息应指明「口令」问题,实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("文件不存在 → 错误信息含明确文件名")
    void load_fileNotFound_messageContainsFileName() {
        // Arrange
        Path missing = FIXTURE_DIR.resolve("does-not-exist.jks");

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new KeyStoreKeyFactory(new FileSystemResource(missing.toFile()), STORE_PASSWORD)
                        .getKeyPair(ALIAS, KEY_PASSWORD));
        assertTrue(ex.getMessage().contains("does-not-exist.jks"),
                "错误信息应含缺失文件名,实际: " + ex.getMessage());
    }

    // ------------------------------------------------------------------
    // RSA 位数门禁
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RSA modulus < 2048 → 拒绝加载")
    void load_weakKeyBelow2048_rejected() {
        // Act & Assert(rsa1024.p12 为 1024 位弱密钥 fixture)
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new KeyStoreKeyFactory(
                        new FileSystemResource(fixture("rsa1024.p12").toFile()), STORE_PASSWORD)
                        .getKeyPair(WEAK_ALIAS, STORE_PASSWORD),
                "弱密钥(1024 位)必须被拒绝");
        assertTrue(ex.getMessage().contains("2048"),
                "拒绝信息应说明最低位数要求,实际: " + ex.getMessage());
    }

    // ------------------------------------------------------------------
    // 资源安全(MEDIUM-2)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("加载后 InputStream 必须关闭(无资源泄漏)")
    void load_inputStreamClosed_afterGetKeyPair() throws IOException {
        // Arrange:fixture 字节 + 关闭跟踪装饰
        byte[] bytes = Files.readAllBytes(fixture("rsa2048.p12"));
        CloseTrackingInputStream tracking = new CloseTrackingInputStream(new ByteArrayInputStream(bytes));
        Resource resource = new InputStreamResource(tracking);

        // Act(PKCS12 单口令)
        new KeyStoreKeyFactory(resource, STORE_PASSWORD).getKeyPair(ALIAS, STORE_PASSWORD);

        // Assert
        assertTrue(tracking.closed, "加载完成后 InputStream 必须关闭(旧实现泄漏)");
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static String fullChainMessages(Throwable t) {
        StringBuilder sb = new StringBuilder(String.valueOf(t.getMessage()));
        for (Throwable c = t.getCause(); c != null; c = c.getCause()) {
            sb.append('\n').append(c.getMessage());
        }
        return sb.toString();
    }

    /** InputStream 关闭跟踪装饰器 */
    private static class CloseTrackingInputStream extends FilterInputStream {
        boolean closed;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
