package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AppNotificationPO;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppNotificationPoConverter 双向映射测试(MapStruct 生成实现)。
 *
 * <p>T023 US4 通知通道数据基础：验证 AppNotification 领域实体与 AppNotificationPO
 * 持久化对象的双向转换完整性(含继承时间戳)与列表顺序保持。</p>
 */
@DisplayName("AppNotificationPoConverter 双向映射")
class AppNotificationPoConverterTest {

    private final AppNotificationPoConverter converter = new AppNotificationPoConverterImpl();

    @Test
    @DisplayName("domain2Po + po2Domain 往返后所有字段(含继承时间戳)保持")
    void roundtrip_allFieldsPreserved() {
        // Arrange
        AppNotification domain = new AppNotification();
        domain.setId(1L);
        domain.setUserId(42);
        domain.setEventType("INDUSTRY_COMPLETE");
        domain.setDedupKey("char-123-20261112-001");
        domain.setCharacterId(123456);
        domain.setCorpId(987654);
        domain.setTitle("工业任务完成");
        domain.setContent("角色 123456 的蓝图制造已完成");
        domain.setIsRead(true);

        Date createTime = new Date(1700000000000L);
        Date modifyTime = new Date(1700001000000L);
        domain.setGmtCreate(createTime);
        domain.setGmtModified(modifyTime);

        // Act: domain -> PO
        AppNotificationPO po = converter.domain2Po(domain);

        // Assert: PO 各字段(含声明字段与继承时间戳)
        assertThat(po.getId()).isEqualTo(1L);
        assertThat(po.getUserId()).isEqualTo(42);
        assertThat(po.getEventType()).isEqualTo("INDUSTRY_COMPLETE");
        assertThat(po.getDedupKey()).isEqualTo("char-123-20261112-001");
        assertThat(po.getCharacterId()).isEqualTo(123456);
        assertThat(po.getCorpId()).isEqualTo(987654);
        assertThat(po.getTitle()).isEqualTo("工业任务完成");
        assertThat(po.getContent()).isEqualTo("角色 123456 的蓝图制造已完成");
        assertThat(po.getIsRead()).isTrue();
        assertThat(po.getGmtCreate()).isEqualTo(createTime);
        assertThat(po.getGmtModified()).isEqualTo(modifyTime);

        // Act: PO -> domain
        AppNotification back = converter.po2Domain(po);

        // Assert: 回转实体各字段
        assertThat(back.getId()).isEqualTo(1L);
        assertThat(back.getUserId()).isEqualTo(42);
        assertThat(back.getEventType()).isEqualTo("INDUSTRY_COMPLETE");
        assertThat(back.getDedupKey()).isEqualTo("char-123-20261112-001");
        assertThat(back.getCharacterId()).isEqualTo(123456);
        assertThat(back.getCorpId()).isEqualTo(987654);
        assertThat(back.getTitle()).isEqualTo("工业任务完成");
        assertThat(back.getContent()).isEqualTo("角色 123456 的蓝图制造已完成");
        assertThat(back.getIsRead()).isTrue();
        assertThat(back.getGmtCreate()).isEqualTo(createTime);
        assertThat(back.getGmtModified()).isEqualTo(modifyTime);
    }

    @Test
    @DisplayName("List 转换保持元素顺序")
    void listConversion_preservesOrder() {
        // Arrange
        AppNotification d1 = new AppNotification();
        d1.setId(10L);
        d1.setUserId(1);
        d1.setEventType("INDUSTRY_COMPLETE");

        AppNotification d2 = new AppNotification();
        d2.setId(20L);
        d2.setUserId(2);
        d2.setEventType("FUEL_LOW");

        AppNotification d3 = new AppNotification();
        d3.setId(30L);
        d3.setUserId(3);
        d3.setEventType("SKILL_QUEUE_EMPTY");

        List<AppNotification> domains = List.of(d1, d2, d3);

        // Act: domain list -> PO list
        List<AppNotificationPO> pos = converter.domain2Po(domains);

        // Assert: 顺序与逐元素 id
        assertThat(pos).hasSize(3);
        assertThat(pos.get(0).getId()).isEqualTo(10L);
        assertThat(pos.get(1).getId()).isEqualTo(20L);
        assertThat(pos.get(2).getId()).isEqualTo(30L);
        assertThat(pos.get(0).getUserId()).isEqualTo(1);
        assertThat(pos.get(1).getUserId()).isEqualTo(2);
        assertThat(pos.get(2).getUserId()).isEqualTo(3);

        // Act: PO list -> domain list
        List<AppNotification> backs = converter.po2Domain(pos);

        // Assert: 回转顺序
        assertThat(backs).hasSize(3);
        assertThat(backs.get(0).getId()).isEqualTo(10L);
        assertThat(backs.get(1).getId()).isEqualTo(20L);
        assertThat(backs.get(2).getId()).isEqualTo(30L);
    }
}
