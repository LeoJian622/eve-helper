package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.dto.system.TaxReturnDTO;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithUserDetails("admin")
@DisplayName("人物钱包交易记录")
class WalletJournalServiceTest {

    @Resource
    WalletJournalService walletJournalService;

    @Test
    void batchInsertOrUpdateFromEsi() throws ParseException {
        walletJournalService.batchInsertOrUpdateFromEsi(2112818290);
    }

    @Test
    void countBoundsReturn() throws ParseException {
        List<TaxReturnDTO> list = walletJournalService.countBoundsReturn("0.15", "0.99", "202411");
        System.out.println("list = " + list);
    }
}