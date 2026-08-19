package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StructurePoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("StructurePoConverter 双向映射")
class StructurePoConverterTest {

    private final StructurePoConverter converter = new StructurePoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        Structure domain = new Structure();
        domain.setStructureId(1023854943968L);
        domain.setCorporationId(987654321L);
        domain.setUserId(37L);

        StructurePO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(37L);

        Structure back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(37L);
    }
}