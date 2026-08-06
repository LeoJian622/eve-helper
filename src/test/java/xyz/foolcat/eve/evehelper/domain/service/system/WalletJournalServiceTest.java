package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.vo.TaxReturnResult;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotUtil;

import java.text.ParseException;
import java.util.List;



@SpringBootTest
@ActiveProfiles("test")
@WithUserDetails("admin")
@DisplayName("人物钱包交易记录")
class WalletJournalServiceTest {

    @Autowired
    WalletJournalService walletJournalService;

    @Test
    void batchInsertOrUpdateFromEsi() throws ParseException {
        walletJournalService.batchInsertOrUpdateFromEsi(2112818290);
    }

    @Test
    void countBoundsReturn() throws ParseException {
//        List<TaxReturnResult> list = walletJournalService.countBoundsReturn("0.15", "0.99", "202411");
        List<TaxReturnResult> taxReturnResults = walletJournalService.countBoundsReturn("0.15", "0.99", "202411");
        StringBuilder message = new StringBuilder("人物\t退税\t");
        for (TaxReturnResult tax :
                taxReturnResults) {
            message.append(tax.name()).append("\t").append(tax.amount()).append("\n");
        }
        JSONObject entries = BotUtil.generateMessage(359635464L, null, "111", message.toString(), false);
        System.out.println("list = " + entries);
    }
}