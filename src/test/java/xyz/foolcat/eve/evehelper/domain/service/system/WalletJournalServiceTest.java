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
import xyz.foolcat.eve.evehelper.application.dto.response.TaxReturnDTO;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotUtil;

import java.text.ParseException;
import java.util.List;


@ActiveProfiles("test")
@SpringBootTest
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
//        List<TaxReturnDTO> list = walletJournalService.countBoundsReturn("0.15", "0.99", "202411");
        List<TaxReturnDTO> taxReturnDTOS = walletJournalService.countBoundsReturn("0.15", "0.99", "202411");
        StringBuilder message = new StringBuilder("人物\t退税\t");
        for (TaxReturnDTO tax :
                taxReturnDTOS) {
            message.append(tax.getName()).append("\t").append(tax.getAmount()).append("\n");
        }
        JSONObject entries = BotUtil.generateMessage(359635464L, null, "111", message.toString(), false);
        System.out.println("list = " + entries);
    }
}