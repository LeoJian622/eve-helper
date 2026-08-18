package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletTransactionPO;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WalletTransactionPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>TDD 红-绿:先引用由 MapStruct 注解处理器生成的 {@code WalletTransactionPoConverterImpl}
 * (main 编译期生成),测试编译失败(RED)证明生成器缺失;实现后(GREEN)校验字段级完整性。</p>
 *
 * <p>类型对齐(plan D5):ownerId 在 domain/PO 均为 {@code Long}(库列 owner_id BIGINT);
 * date 为 {@code OffsetDateTime};division/typeId/quantity/clientId 为 {@code Integer}。</p>
 */
@DisplayName("WalletTransactionPoConverter 双向映射")
class WalletTransactionPoConverterTest {

    private final WalletTransactionPoConverter converter = new WalletTransactionPoConverterImpl();

    @Test
    @DisplayName("TODO red-scaffold - 转换器生成实现应存在(先红后绿)")
    void scaffold_converterImpl_exists() {
        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("domain2Po 完整映射字段级一致(含 date OffsetDateTime / ownerId Long)")
    void domain2Po_mapsAllFields() {
        WalletTransaction domain = new WalletTransaction();
        domain.setOwnerType("character");
        domain.setOwnerId(100L);
        domain.setDivision(0);
        domain.setTransactionId(9001L);
        domain.setDate(OffsetDateTime.of(2026, 1, 2, 10, 30, 0, 0, ZoneOffset.UTC));
        domain.setTypeId(14002);
        domain.setQuantity(5);
        domain.setUnitPrice(99.5);
        domain.setClientId(2112832425);
        domain.setLocationId(60003760L);
        domain.setIsBuy(Boolean.TRUE);
        domain.setIsPersonal(Boolean.FALSE);
        domain.setJournalRefId(123456L);

        WalletTransactionPO po = converter.domain2Po(domain);

        assertThat(po.getOwnerType()).isEqualTo("character");
        assertThat(po.getOwnerId()).isEqualTo(100L);
        assertThat(po.getDivision()).isEqualTo(0);
        assertThat(po.getTransactionId()).isEqualTo(9001L);
        assertThat(po.getDate()).isEqualTo(OffsetDateTime.of(2026, 1, 2, 10, 30, 0, 0, ZoneOffset.UTC));
        assertThat(po.getTypeId()).isEqualTo(14002);
        assertThat(po.getQuantity()).isEqualTo(5);
        assertThat(po.getUnitPrice()).isEqualTo(99.5);
        assertThat(po.getClientId()).isEqualTo(2112832425);
        assertThat(po.getLocationId()).isEqualTo(60003760L);
        assertThat(po.getIsBuy()).isTrue();
        assertThat(po.getIsPersonal()).isFalse();
        assertThat(po.getJournalRefId()).isEqualTo(123456L);
    }

    @Test
    @DisplayName("po2Domain + domain2Po 往返后关键字段保持(幂等映射)")
    void roundTrip_preservesKeyFields() {
        WalletTransactionPO po = new WalletTransactionPO();
        po.setOwnerType("corporation");
        po.setOwnerId(777L);
        po.setDivision(2);
        po.setTransactionId(456L);
        po.setDate(OffsetDateTime.of(2026, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC));
        po.setUnitPrice(12.25);
        po.setIsBuy(Boolean.FALSE);

        WalletTransaction back = converter.po2Domain(po);

        assertThat(back.getOwnerType()).isEqualTo("corporation");
        assertThat(back.getOwnerId()).isEqualTo(777L);
        assertThat(back.getDivision()).isEqualTo(2);
        assertThat(back.getTransactionId()).isEqualTo(456L);
        assertThat(back.getDate()).isEqualTo(OffsetDateTime.of(2026, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC));
        assertThat(back.getUnitPrice()).isEqualTo(12.25);
        assertThat(back.getIsBuy()).isFalse();
    }

    @Test
    @DisplayName("List 重载整体转换保持一致")
    void listOverload_mapsAllElements() {
        WalletTransaction a = new WalletTransaction();
        a.setTransactionId(1L);
        WalletTransaction b = new WalletTransaction();
        b.setTransactionId(2L);

        List<WalletTransactionPO> pos = converter.domain2Po(List.of(a, b));

        assertThat(pos).hasSize(2);
        List<WalletTransaction> back = converter.po2Domain(pos);
        assertThat(back).extracting(WalletTransaction::getTransactionId).containsExactly(1L, 2L);
    }
}