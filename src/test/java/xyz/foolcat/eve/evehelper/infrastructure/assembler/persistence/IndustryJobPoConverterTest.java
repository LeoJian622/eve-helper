package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IndustryJobPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("IndustryJobPoConverter 双向映射")
class IndustryJobPoConverterTest {

    private final IndustryJobPoConverter converter = new IndustryJobPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        IndustryJob domain = new IndustryJob();
        domain.setJobId(123456L);
        domain.setCorporationId(987654321);
        domain.setUserId(39L);

        IndustryJobPO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(39L);

        IndustryJob back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(39L);
    }
}