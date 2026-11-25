package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AppNotificationPO;

import java.util.Date;
import java.util.List;

/**
 * APP 通知 MyBatis Mapper（基础设施层）。
 * <p>
 * 继承 BaseMapper 提供 MP Wrapper CRUD；XML 仅承载 insertBatch 与 deleteExpired。
 *
 * @author Leojian
 * date 2026-11-13
 */
@Mapper
public interface AppNotificationMapper extends BaseMapper<AppNotificationPO> {

    /**
     * 批量插入通知（XML foreach，省略 id/gmt_create/gmt_modified 三列）。
     *
     * @param list 通知 PO 列表
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<AppNotificationPO> list);

    /**
     * 分批删除过期通知（WHERE gmt_create &lt; cutoff LIMIT batchSize）。
     *
     * @param cutoff    截止时间
     * @param batchSize 每批删除上限
     * @return 本次删除行数
     */
    int deleteExpired(@Param("cutoff") Date cutoff, @Param("batchSize") int batchSize);
}
