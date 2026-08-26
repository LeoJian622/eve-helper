package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import java.time.Duration;

/**
 * ESI 客户端公共常量。
 *
 * <p>提取各 API 类中重复声明的常量,统一在此处定义,避免 20+ 处重复。</p>
 *
 * @author Leojan
 * @since 2026-08-26
 */
public final class EsiClientConstants {

    private EsiClientConstants() {
        // 静态工具类,禁止实例化
    }

    /**
     * ESI 数据接口调用超时(宪法:外部调用 5s 超时)。
     *
     * <p>原分散声明于 33 个 API 类及 EsiApiService 中,现统一引用此常量。</p>
     */
    public static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);
}
