package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MiningDetailPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("MiningDetailPoConverter 双向映射")
class MiningDetailPoConverterTest {

    private final MiningDetailPoConverter converter = new MiningDetailPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        MiningDetail domain = new MiningDetail();
        domain.setId("sync-mining-0001");
        domain.setCharacterId(2112832425);
        domain.setUserId(40L);

        MiningDetailPO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(40L);

        MiningDetail back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(40L);
    }
}