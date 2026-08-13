package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SecurityContextHolder 策略回归测试(008 T005)。
 *
 * <p>验证策略为默认 ThreadLocal 策略(非继承的 InheritableThreadLocal 策略)。
 * 配合 grep 复核 {@code SecurityConfig} 无 {@code setStrategyName} 覆写,共同证明继承策略已移除,
 * 池化线程不再在创建时继承提交线程身份(007 MEDIUM-6 修复的源头切断)。
 */
class SecurityContextStrategyTest {

    @Test
    void strategyIsThreadLocalNotInheritable() {
        String strategy = SecurityContextHolder.getContextHolderStrategy().getClass().getSimpleName();
        assertTrue(strategy.contains("ThreadLocal"),
                "必须使用默认 ThreadLocal 策略,实际: " + strategy);
        assertFalse(strategy.contains("Inheritable"),
                "不得使用继承策略 InheritableThreadLocal,实际: " + strategy);
    }
}