package xyz.foolcat.eve.evehelper.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 这是一个用于测试钱包记录定时任务（WalletTask）的集成测试类。
 * 该类使用Spring Boot提供的测试框架和JUnit Jupiter作为测试引擎，
 * 通过注解配置了一个真实的Web环境，并自动注入被测对象（WalletTask）进行功能验证。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("钱包记录定时任务测试")
class WalletTaskTest {

    /**
     * 自动注入的_walletTask实例，用于在测试方法中调用其相关方法。
     */
    @Autowired
    private WalletTask walletTask;

    /**
     * 测试钱包记录更新功能的方法。
     * 该方法调用_walletTask的updateWallet()方法，验证定时任务是否能够正确执行。
     */
    @Test
    void updateWallet() {
        walletTask.updateWallet();
    }
}