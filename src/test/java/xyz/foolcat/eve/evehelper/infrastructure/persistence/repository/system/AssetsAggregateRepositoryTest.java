package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsAggregateVO;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 资产多角色聚合 SQL 语义集成测试。
 *
 * <p>该表仅 owner_id=2112832425 已回填 ownerId 且有资产(EVE 测试库现状),
 * 直接对真实数据断言聚合 SQL 语义:件数求和、价值 = quantity * base_price 求和(联 inv_types)、
 * 类目数 = 去重 type_id。无资产 owner 返回 null。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("资产聚合仓储集成测试")
class AssetsAggregateRepositoryTest {

    /**
     * 测试库中唯一已回填 ownerId 的资产 owner(会话期经 IDE 核实)。
     */
    private static final int BACKFILLED_OWNER = 2112832425;

    @Autowired
    AssetsRepository assetsRepository;

    @Test
    @DisplayName("有资产 owner -> 返回件数/价值/类目数聚合")
    void aggregate_returnsSums() {
        AssetsAggregateVO agg = assetsRepository.acquireAggregateByOwnerId(BACKFILLED_OWNER);

        assertNotNull(agg);
        assertEquals(BACKFILLED_OWNER, agg.ownerId());
        // 以下基准值来自 inv_types JOIN 后同类聚合 SQL(IDE 实测确认)
        assertEquals(75461076L, agg.assetCount());
        assertEquals(2509365997597.0, agg.assetValue(), 1.0);
        assertEquals(902L, agg.categoryCount());
    }

    @Test
    @DisplayName("有资产 owner -> 返回非零真实聚合")
    void aggregate_positiveRealValues() {
        AssetsAggregateVO agg = assetsRepository.acquireAggregateByOwnerId(BACKFILLED_OWNER);

        assertNotNull(agg);
        assertEquals(BACKFILLED_OWNER, agg.ownerId());
        // 不硬编码精确值,只断言聚合的数值语义成立
        assertEquals(true, MoreThanZero(agg.assetCount()));
        assertEquals(true, MoreThanZero(agg.assetValue()));
        assertEquals(true, MoreThanZero(agg.categoryCount()));
    }

    @Test
    @DisplayName("无资产 owner -> 返回 null")
    void aggregate_noAssets_returnsNull() {
        assertNull(assetsRepository.acquireAggregateByOwnerId(9_998_999));
    }

    @Test
    @DisplayName("聚合归组 owner && 类目数为正的自检")
    void aggregate_notNullAndOwnerGrouped() {
        AssetsAggregateVO agg = assetsRepository.acquireAggregateByOwnerId(BACKFILLED_OWNER);
        assertNotNull(agg);
        assertEquals(BACKFILLED_OWNER, agg.ownerId());
        assertEquals(true, MoreThanZero(agg.categoryCount()));
    }

    private static boolean MoreThanZero(Long v) {
        return v != null && v > 0;
    }

    private static boolean MoreThanZero(Double v) {
        return v != null && v > 0;
    }
}