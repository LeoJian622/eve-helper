/*
 * Copyright 20013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

/**
 * Factory for RSA key pairs from a JKS/PKCS12 keystore file(007 FR-023,T022 重写)。
 *
 * <p><b>重写要点(对照旧实现缺陷)</b>:</p>
 * <ul>
 *   <li><b>资源安全(MEDIUM-2)</b>:try-with-resources 关闭 InputStream(旧实现泄漏)</li>
 *   <li><b>无状态</b>:去除双重 synchronized 与可变 {@code store} 字段 —— 工厂仅在
 *       启动期被调用一次,无并发复用需求,不可变字段即够</li>
 *   <li><b>异常分类</b>:文件不存在 / 口令错误 / 别名错误 / 非 RSA / 位数不足
 *       各给独立可排障信息,且<b>任何信息不回显口令原文</b>(M-2)</li>
 *   <li><b>RSA 位数门禁</b>:modulus &lt; 2048 拒绝加载(弱密钥防御)</li>
 *   <li><b>双物理格式(M-4)</b>:沿用 {@code getInstance("jks")} 的 JDK DualFormat
 *       兼容加载 —— 同时支持 JKS 与 PKCS12 物理格式(现 test-only.jks 实为
 *       PKCS12,此前即靠该机制加载,重写不得破坏,契约测试双 fixture 守护)</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Leojan (007 重写)
 */
public class KeyStoreKeyFactory {

    /** RSA 最低位数门禁 */
    private static final int MIN_RSA_MODULUS_BITS = 2048;

    private final Resource resource;

    private final char[] password;

    public KeyStoreKeyFactory(Resource resource, char[] password) {
        this.resource = resource;
        this.password = password;
    }

    public KeyPair getKeyPair(String alias) {
        return getKeyPair(alias, password);
    }

    public KeyPair getKeyPair(String alias, char[] keyPassword) {
        KeyStore store = loadKeyStore();
        RSAPrivateCrtKey privateKey = extractRsaKey(store, alias, keyPassword);

        int bits = privateKey.getModulus().bitLength();
        if (bits < MIN_RSA_MODULUS_BITS) {
            throw new IllegalStateException(
                    "keystore 密钥位数不足: RSA modulus 最低要求 2048 位,别名 "
                            + alias + " 实际仅 " + bits + " 位,拒绝加载");
        }

        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("keystore 公钥推导失败: 别名 " + alias, e);
        }
    }

    /**
     * 加载 keystore(DualFormat:JKS 与 PKCS12 物理格式均可)。
     * 异常分类:文件不存在 / 口令错误或文件损坏,信息不含口令原文。
     */
    private KeyStore loadKeyStore() {
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "keystore 文件不存在: " + describe() + " —— 请核对 security.keystore.location 配置");
        }
        try (InputStream in = resource.getInputStream()) {
            KeyStore store = KeyStore.getInstance("jks");
            store.load(in, password);
            return store;
        } catch (IOException e) {
            // 口令错误时 JDK 抛 IOException(如 "keystore password was incorrect");
            // 文件损坏亦在此路径。统一为可排障信息,绝不回显口令。
            throw new IllegalStateException(
                    "keystore 加载失败: 口令不匹配或文件已损坏(" + describe()
                            + ")。请核对 KEYSTORE_PASSWORD 与文件完整性", e);
        } catch (Exception e) {
            throw new IllegalStateException("keystore 加载失败: " + describe(), e);
        }
    }

    /**
     * 提取 RSA 私钥。异常分类:别名不存在 / 口令不匹配 / 非 RSA 密钥。
     */
    private RSAPrivateCrtKey extractRsaKey(KeyStore store, String alias, char[] keyPassword) {
        final java.security.Key key;
        try {
            if (!store.containsAlias(alias)) {
                throw new IllegalStateException(
                        "keystore 中不存在该别名: 请求别名 " + alias
                                + " —— 请核对 security.keystore.alias 配置(KEYSTORE_ALIAS)");
            }
            key = store.getKey(alias, keyPassword);
        } catch (IllegalStateException e) {
            throw e;
        } catch (UnrecoverableKeyException e) {
            throw new IllegalStateException(
                    "keystore 条目口令不匹配: 别名 " + alias
                            + " —— 请核对 KEY_PASSWORD(错误信息不含口令原文)", e);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("keystore 读取失败: 别名 " + alias, e);
        }

        if (!(key instanceof RSAPrivateCrtKey rsaKey)) {
            throw new IllegalStateException(
                    "keystore 密钥类型错误: 别名 " + alias + " 不是 RSA 私钥,无法用于 JWT RS256 签名");
        }
        return rsaKey;
    }

    /** 只暴露文件名,不暴露完整路径(LOW-5) */
    private String describe() {
        String filename = resource.getFilename();
        return filename != null ? filename : resource.getDescription();
    }

}
