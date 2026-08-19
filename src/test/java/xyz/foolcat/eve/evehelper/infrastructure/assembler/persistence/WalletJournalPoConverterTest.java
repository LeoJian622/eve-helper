package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WalletJournalPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("WalletJournalPoConverter 双向映射")
class WalletJournalPoConverterTest {

    private final WalletJournalPoConverter converter = new WalletJournalPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        WalletJournal domain = new WalletJournal();
        domain.setOwnerId(2112832425L);
        domain.setDivision(0);
        domain.setUserId(36L);

        WalletJournalPO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(36L);

        WalletJournal back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(36L);
    }
}