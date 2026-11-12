package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AppNotificationPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.AppNotificationMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppNotification 仓储集成测试（T024）。
 * <p>
 * 真实测试库 eve_helper_test（阿里云 RDS），app_notification 表已建。
 * 测试隔离：合成 userId 999022001/999022002，dedup_key 前缀 T024TEST:，
 * 无 @Transactional（多数据源事务管理器歧义），@AfterEach 用 mapper 清理。
 *
 * @author Leojian
 * date 2026-11-13
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("T024 APP 通知仓储集成测试")
class AppNotificationRepositoryImplTest {

    private static final Integer TEST_USER_1 = 999022001;
    private static final Integer TEST_USER_2 = 999022002;
    private static final String DEDUP_PREFIX = "T024TEST:";

    @Autowired
    AppNotificationRepository appNotificationRepository;

    @Autowired
    AppNotificationMapper appNotificationMapper;

    @AfterEach
    void cleanup() {
        appNotificationMapper.delete(
                new UpdateWrapper<AppNotificationPO>()
                        .in("user_id", Arrays.asList(TEST_USER_1, TEST_USER_2))
        );
    }

    // ─── 辅助方法 ──────────────────────────────────────────

    private AppNotification buildNotification(Integer userId, String dedupKey, String title) {
        AppNotification n = new AppNotification();
        n.setUserId(userId);
        n.setEventType("INDUSTRY_COMPLETE");
        n.setDedupKey(dedupKey);
        n.setCharacterId(100001);
        n.setCorpId(200001);
        n.setTitle(title);
        n.setContent("Test content for " + dedupKey);
        n.setIsRead(false);
        return n;
    }

    // ─── 用例 1: insertBatch_persistsAllRows ────────────────

    @Test
    @DisplayName("insertBatch 批量插入三行并逐字段断言")
    void insertBatch_persistsAllRows() {
        AppNotification n1 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "batch1", "Title1");
        AppNotification n2 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "batch2", "Title2");
        AppNotification n3 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "batch3", "Title3");

        int inserted = appNotificationRepository.insertBatch(Arrays.asList(n1, n2, n3));
        assertEquals(3, inserted);

        // 查回验证
        PageResult<AppNotification> page = appNotificationRepository.pageByUser(
                TEST_USER_1, null, PageQuery.builder().current(1).size(10).build());
        assertEquals(3L, page.getTotal());

        List<AppNotification> records = page.getRecords();
        for (AppNotification r : records) {
            assertNotNull(r.getId());
            assertEquals(TEST_USER_1, r.getUserId());
            assertEquals("INDUSTRY_COMPLETE", r.getEventType());
            assertTrue(r.getDedupKey().startsWith(DEDUP_PREFIX));
            assertEquals(100001, r.getCharacterId());
            assertEquals(200001, r.getCorpId());
            assertNotNull(r.getTitle());
            assertNotNull(r.getContent());
            assertFalse(r.getIsRead());
            // DB DEFAULT 时间戳由数据库生成
            assertNotNull(r.getGmtCreate(), "gmt_create should be set by DB DEFAULT");
            assertNotNull(r.getGmtModified(), "gmt_modified should be set by DB DEFAULT");
        }
    }

    // ─── 用例 2: existsByDedupKey_ignoresTimeWindow ─────────

    @Test
    @DisplayName("existsByDedupKey 无时间窗口：48h 前仍 true；不存在的 key false")
    void existsByDedupKey_ignoresTimeWindow() {
        AppNotification n = buildNotification(TEST_USER_1, DEDUP_PREFIX + "once", "Once");
        appNotificationRepository.insertBatch(List.of(n));

        // 把 gmt_create 调到 48h 前
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -48);
        Date oldDate = cal.getTime();

        AppNotificationPO found = appNotificationMapper.selectOne(
                new UpdateWrapper<AppNotificationPO>()
                        .eq("user_id", TEST_USER_1)
                        .eq("dedup_key", DEDUP_PREFIX + "once")
        );
        assertNotNull(found);
        appNotificationMapper.update(null,
                new UpdateWrapper<AppNotificationPO>()
                        .eq("id", found.getId())
                        .set("gmt_create", oldDate)
        );

        assertTrue(appNotificationRepository.existsByDedupKey(DEDUP_PREFIX + "once"),
                "48h 前的 dedup_key 仍应返回 true（一次性事件永久去重）");
        assertFalse(appNotificationRepository.existsByDedupKey(DEDUP_PREFIX + "nonexistent"),
                "不存在的 key 应返回 false");
    }

    // ─── 用例 3: existsByDedupKeyWithinHours_boundary24h ────

    @Test
    @DisplayName("existsByDedupKeyWithinHours 24h 边界：23h59m 前 true，25h 前 false")
    void existsByDedupKeyWithinHours_boundary24h() {
        // 插入两行不同 key
        AppNotification nA = buildNotification(TEST_USER_1, DEDUP_PREFIX + "withinA", "A");
        AppNotification nB = buildNotification(TEST_USER_1, DEDUP_PREFIX + "withinB", "B");
        appNotificationRepository.insertBatch(Arrays.asList(nA, nB));

        // A 行调到 23h59m 前
        Calendar calA = Calendar.getInstance();
        calA.add(Calendar.HOUR_OF_DAY, -23);
        calA.add(Calendar.MINUTE, -59);
        Date dateA = calA.getTime();

        // B 行调到 25h 前
        Calendar calB = Calendar.getInstance();
        calB.add(Calendar.HOUR_OF_DAY, -25);
        Date dateB = calB.getTime();

        AppNotificationPO poA = appNotificationMapper.selectOne(
                new UpdateWrapper<AppNotificationPO>()
                        .eq("user_id", TEST_USER_1)
                        .eq("dedup_key", DEDUP_PREFIX + "withinA")
        );
        appNotificationMapper.update(null,
                new UpdateWrapper<AppNotificationPO>()
                        .eq("id", poA.getId())
                        .set("gmt_create", dateA)
        );

        AppNotificationPO poB = appNotificationMapper.selectOne(
                new UpdateWrapper<AppNotificationPO>()
                        .eq("user_id", TEST_USER_1)
                        .eq("dedup_key", DEDUP_PREFIX + "withinB")
        );
        appNotificationMapper.update(null,
                new UpdateWrapper<AppNotificationPO>()
                        .eq("id", poB.getId())
                        .set("gmt_create", dateB)
        );

        assertTrue(appNotificationRepository.existsByDedupKeyWithinHours(DEDUP_PREFIX + "withinA", 24),
                "23h59m 前在 24h 窗口内，应返回 true");
        assertFalse(appNotificationRepository.existsByDedupKeyWithinHours(DEDUP_PREFIX + "withinB", 24),
                "25h 前超出 24h 窗口，应返回 false");
    }

    // ─── 用例 4: pageByUser_unreadOnlyFilters ───────────────

    @Test
    @DisplayName("pageByUser unreadOnly=true 只返未读；false/null 返全部")
    void pageByUser_unreadOnlyFilters() {
        // 2 未读 + 1 已读
        AppNotification n1 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "unread1", "U1");
        AppNotification n2 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "unread2", "U2");
        AppNotification n3 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "read1", "R1");
        n3.setIsRead(true);
        appNotificationRepository.insertBatch(Arrays.asList(n1, n2, n3));

        // unreadOnly=true → 2 条
        PageResult<AppNotification> unreadPage = appNotificationRepository.pageByUser(
                TEST_USER_1, true, PageQuery.builder().current(1).size(10).build());
        assertEquals(2L, unreadPage.getTotal(), "unreadOnly=true 应返回 2 条未读");

        // unreadOnly=false → 3 条
        PageResult<AppNotification> allPage = appNotificationRepository.pageByUser(
                TEST_USER_1, false, PageQuery.builder().current(1).size(10).build());
        assertEquals(3L, allPage.getTotal(), "unreadOnly=false 应返回全部 3 条");

        // unreadOnly=null → 3 条
        PageResult<AppNotification> nullPage = appNotificationRepository.pageByUser(
                TEST_USER_1, null, PageQuery.builder().current(1).size(10).build());
        assertEquals(3L, nullPage.getTotal(), "unreadOnly=null 应返回全部 3 条");
    }

    // ─── 用例 5: pageByUser_isolatedByUserId ────────────────

    @Test
    @DisplayName("pageByUser 按 userId 隔离：user1 只含 user1 行")
    void pageByUser_isolatedByUserId() {
        AppNotification n1 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "iso1", "Iso1");
        AppNotification n2 = buildNotification(TEST_USER_2, DEDUP_PREFIX + "iso2", "Iso2");
        appNotificationRepository.insertBatch(Arrays.asList(n1, n2));

        PageResult<AppNotification> user1Page = appNotificationRepository.pageByUser(
                TEST_USER_1, null, PageQuery.builder().current(1).size(10).build());
        assertEquals(1L, user1Page.getTotal(), "user1 应只有 1 条");
        assertEquals(TEST_USER_1, user1Page.getRecords().get(0).getUserId());

        PageResult<AppNotification> user2Page = appNotificationRepository.pageByUser(
                TEST_USER_2, null, PageQuery.builder().current(1).size(10).build());
        assertEquals(1L, user2Page.getTotal(), "user2 应只有 1 条");
        assertEquals(TEST_USER_2, user2Page.getRecords().get(0).getUserId());
    }

    // ─── 用例 6: markRead_otherUsersRow_returnsZero ─────────

    @Test
    @DisplayName("markRead 他人行返回 0 且仍未读；自己行返回 1 且已读")
    void markRead_otherUsersRow_returnsZero() {
        AppNotification n = buildNotification(TEST_USER_1, DEDUP_PREFIX + "markread", "MR");
        appNotificationRepository.insertBatch(List.of(n));

        AppNotificationPO inserted = appNotificationMapper.selectOne(
                new UpdateWrapper<AppNotificationPO>()
                        .eq("user_id", TEST_USER_1)
                        .eq("dedup_key", DEDUP_PREFIX + "markread")
        );
        assertNotNull(inserted);
        Long rowId = inserted.getId();

        // user2 尝试标 user1 的行 → 返回 0
        int affected = appNotificationRepository.markRead(rowId, TEST_USER_2);
        assertEquals(0, affected, "他人行 markRead 应返回 0");

        // 验证行仍未读
        AppNotificationPO stillUnread = appNotificationMapper.selectById(rowId);
        assertFalse(stillUnread.getIsRead(), "他人 markRead 后行仍应未读");

        // user1 标自己的行 → 返回 1
        int selfAffected = appNotificationRepository.markRead(rowId, TEST_USER_1);
        assertEquals(1, selfAffected, "自己行 markRead 应返回 1");

        // 验证已读
        AppNotificationPO nowRead = appNotificationMapper.selectById(rowId);
        assertTrue(nowRead.getIsRead(), "markRead 后行应已读");
    }

    // ─── 用例 7: deleteExpired_batchedDelete ────────────────

    @Test
    @DisplayName("deleteExpired 分批删除：3 过期+1 新行，首次 batchSize=2 删 2，再调删余 1，新行保留")
    void deleteExpired_batchedDelete() {
        // 3 条过期 + 1 条新行
        AppNotification expired1 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "exp1", "E1");
        AppNotification expired2 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "exp2", "E2");
        AppNotification expired3 = buildNotification(TEST_USER_1, DEDUP_PREFIX + "exp3", "E3");
        AppNotification fresh = buildNotification(TEST_USER_1, DEDUP_PREFIX + "fresh", "F");
        appNotificationRepository.insertBatch(Arrays.asList(expired1, expired2, expired3, fresh));

        // 将 3 条过期行 gmt_create 调到 31 天前
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -31);
        Date oldDate = cal.getTime();

        List<String> expiredKeys = List.of(
                DEDUP_PREFIX + "exp1", DEDUP_PREFIX + "exp2", DEDUP_PREFIX + "exp3");
        for (String key : expiredKeys) {
            AppNotificationPO po = appNotificationMapper.selectOne(
                    new UpdateWrapper<AppNotificationPO>()
                            .eq("user_id", TEST_USER_1)
                            .eq("dedup_key", key)
            );
            appNotificationMapper.update(null,
                    new UpdateWrapper<AppNotificationPO>()
                            .eq("id", po.getId())
                            .set("gmt_create", oldDate)
            );
        }

        // 首次 deleteExpired(30, 2) → 删 2 条（LIMIT 2 生效）
        int first = appNotificationRepository.deleteExpired(30, 2);
        assertEquals(2, first, "首次分批删 2 条");

        // 再次 → 删余 1 条过期
        int second = appNotificationRepository.deleteExpired(30, 2);
        assertEquals(1, second, "第二次删余 1 条过期");

        // 新行仍在
        PageResult<AppNotification> remaining = appNotificationRepository.pageByUser(
                TEST_USER_1, null, PageQuery.builder().current(1).size(10).build());
        assertEquals(1L, remaining.getTotal(), "新行应保留");
        assertEquals(DEDUP_PREFIX + "fresh", remaining.getRecords().get(0).getDedupKey());

        // 第三次 → 0
        int third = appNotificationRepository.deleteExpired(30, 2);
        assertEquals(0, third, "无过期行时应返回 0");
    }
}
