package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlueprintsPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>014 user_id:声明 domain.setUserId/PO.getUserId 先于字段存在,引用缺失字段导致
 * 编译失败(RED);加入 {@code Long userId} 后(GREEN)校验 PO↔domain userId 往返保留。</p>
 */
@DisplayName("BlueprintsPoConverter 双向映射")
class BlueprintsPoConverterTest {

    private final BlueprintsPoConverter converter = new BlueprintsPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后 userId 保持")
    void roundTrip_preservesUserId() {
        Blueprints domain = new Blueprints();
        domain.setItemId(1017113140391L);
        domain.setOwnerId(2112832425L);
        domain.setUserId(41L);

        BlueprintsPO po = converter.domain2Po(domain);
        assertThat(po.getUserId()).isEqualTo(41L);

        Blueprints back = converter.po2Domain(po);
        assertThat(back.getUserId()).isEqualTo(41L);
    }
}