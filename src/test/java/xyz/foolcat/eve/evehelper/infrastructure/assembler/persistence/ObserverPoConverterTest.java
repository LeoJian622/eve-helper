package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.ObserverPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObserverPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("ObserverPoConverter 双向映射")
class ObserverPoConverterTest {

    private final ObserverPoConverter converter = new ObserverPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        Observer domain = new Observer();
        domain.setObserverId(1014012914900L);
        domain.setCorporationId(987654321L);
        domain.setUserId(38L);

        ObserverPO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(38L);

        Observer back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(38L);
    }
}