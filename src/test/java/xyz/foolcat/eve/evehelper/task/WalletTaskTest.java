package xyz.foolcat.eve.evehelper.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("钱包记录定时任务测试")
class WalletTaskTest {

    @Autowired
    private WalletTask walletTask;

    @Test
    void updateWallet() {
        walletTask.updateWallet();
    }
}