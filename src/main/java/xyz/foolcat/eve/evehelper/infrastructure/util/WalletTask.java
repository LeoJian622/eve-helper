package xyz.foolcat.eve.evehelper.infrastructure.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;

import java.text.ParseException;

/**
 * @author Leojan
 * date 2023-07-30 9:05
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletTask {

    private final WalletJournalService walletJournalService;

    /**
     * 更新钱包记录
     */
    @Scheduled(cron = "0 0 0 1/15 * ? ")
    public void updateWallet() {
        log.info("updateWallet");
        try {
            walletJournalService.batchInsertOrUpdateFromEsi(2112818290);
        } catch (ParseException e) {
            log.error("【钱包】定时更新钱包记录失败：{}", e.getMessage());
        }
    }

}
