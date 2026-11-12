package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.AppNotificationPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AppNotificationPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.AppNotificationMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * APP 通知仓储实现（基础设施层）。
 * <p>
 * insertBatch / deleteExpired 走 XML（批量 + LIMIT 分批）；
 * 其余五方法用 MyBatis-Plus Wrapper，全参数化，不写 XML。
 * markRead 归属内建 WHERE（WHERE id=? AND user_id=?），他人行返回 0。
 *
 * @author Leojian
 * date 2026-11-13
 */
@Repository
@RequiredArgsConstructor
public class AppNotificationRepositoryImpl implements AppNotificationRepository {

    private static final int BATCH_SIZE = 500;

    private final AppNotificationMapper appNotificationMapper;
    private final AppNotificationPoConverter appNotificationPoConverter;

    @Override
    public int insertBatch(List<AppNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return 0;
        }
        List<AppNotificationPO> poList = appNotificationPoConverter.domain2Po(notifications);
        int total = 0;
        for (int i = 0; i < poList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, poList.size());
            total += appNotificationMapper.insertBatch(poList.subList(i, end));
        }
        return total;
    }

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        Long count = appNotificationMapper.selectCount(
                new QueryWrapper<AppNotificationPO>()
                        .eq("dedup_key", dedupKey)
        );
        return count != null && count > 0;
    }

    @Override
    public boolean existsByDedupKeyWithinHours(String dedupKey, int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        Date cutoff = cal.getTime();

        Long count = appNotificationMapper.selectCount(
                new QueryWrapper<AppNotificationPO>()
                        .eq("dedup_key", dedupKey)
                        .ge("gmt_create", cutoff)
        );
        return count != null && count > 0;
    }

    @Override
    public PageResult<AppNotification> pageByUser(Integer userId, Boolean unreadOnly, PageQuery pageQuery) {
        QueryWrapper<AppNotificationPO> wrapper = new QueryWrapper<AppNotificationPO>()
                .eq("user_id", userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq("is_read", false);
        }
        wrapper.orderByDesc("gmt_create");

        Page<AppNotificationPO> page = new Page<>(pageQuery.getCurrent(), pageQuery.getSize());
        Page<AppNotificationPO> result = appNotificationMapper.selectPage(page, wrapper);

        List<AppNotification> records = appNotificationPoConverter.po2Domain(result.getRecords());

        return PageResult.<AppNotification>builder()
                .records(records)
                .total(result.getTotal())
                .current(result.getCurrent())
                .size(result.getSize())
                .pages(result.getPages())
                .hasNext(result.getPages() - result.getCurrent() > 0)
                .hasPrevious(result.getCurrent() > 1)
                .build();
    }

    @Override
    public long countUnread(Integer userId) {
        Long count = appNotificationMapper.selectCount(
                new QueryWrapper<AppNotificationPO>()
                        .eq("user_id", userId)
                        .eq("is_read", false)
        );
        return count != null ? count : 0L;
    }

    @Override
    public int markRead(Long id, Integer userId) {
        return appNotificationMapper.update(null,
                new UpdateWrapper<AppNotificationPO>()
                        .set("is_read", true)
                        .eq("id", id)
                        .eq("user_id", userId)
        );
    }

    @Override
    public int deleteExpired(int retentionDays, int batchSize) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -retentionDays);
        Date cutoff = cal.getTime();
        return appNotificationMapper.deleteExpired(cutoff, batchSize);
    }
}
