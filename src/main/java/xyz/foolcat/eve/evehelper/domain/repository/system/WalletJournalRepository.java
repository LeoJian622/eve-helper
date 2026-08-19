package xyz.foolcat.eve.evehelper.domain.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewAggregate;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Leojan
 */
public interface WalletJournalRepository {
    int updateBatch(List<WalletJournal> list);

    int updateBatchSelective(List<WalletJournal> list);

    int batchInsert(List<WalletJournal> list);

    public boolean insertOrUpdate(WalletJournal record);

    int insertOrUpdateSelective(WalletJournal record);

    void saveOrUpdateBatch(List<WalletJournal> walletJournals);

    List<Map<String, Object>> selectMapByDatetime(Date start, Date end, List<String> refType);

    /**
     * 按所有者ID分页查询钱包流水(时间 date 倒序)。
     *
     * @param page    MyBatis Plus 分页参数(领域实体维度;PO 分页由实现内聚)
     * @param ownerId 所有者ID(人物ID 或军团ID)
     * @return 领域实体分页结果(total 由分页插件填充)
     */
    IPage<WalletJournal> selectPageByOwnerId(IPage<WalletJournal> page, Long ownerId);

    /**
     * 按所有者ID与分账分页查询钱包流水(时间 date 倒序)。
     *
     * @param page     MyBatis Plus 分页参数
     * @param ownerId  所有者ID
     * @param division 分账(人物=0,军团=1-7)
     * @param userId   军团维读过滤(US2b):null=ROOT 看全量不过滤,非 null 按 user_id 过滤;人物读取传 null
     * @return 领域实体分页结果
     */
    IPage<WalletJournal> selectPageByOwnerAndDivision(IPage<WalletJournal> page, Long ownerId, Integer division, Long userId);

    /**
     * 钱包总览标量聚合(收支/净流/条数/当前余额/统计截点)。
     *
     * @param ownerId  所有者ID(人物或军团)
     * @param division 分账;人物传 null 不过滤
     * @param start    起始时间(可空)
     * @param end      结束时间(可空)
     * @return 标量聚合(无数据时金额/条数为 0)
     */
    WalletOverviewAggregate selectOverviewAggregate(Long ownerId, Integer division, Long userId, Date start, Date end);

    /**
     * 钱包总览按交易类型汇总。
     *
     * @param ownerId  所有者ID
     * @param division 分账;人物传 null 不过滤
     * @param start    起始时间(可空)
     * @param end      结束时间(可空)
     * @return 类型汇总列表
     */
    List<WalletOverviewVO.CategorySummary> selectOverviewCategories(Long ownerId, Integer division, Long userId, Date start, Date end);

    /**
     * 钱包总览按时间桶趋势。
     *
     * @param ownerId     所有者ID
     * @param division    分账;人物传 null 不过滤
     * @param start       起始时间(可空)
     * @param end         结束时间(可空)
     * @param granularity 时间桶格式(月/日)
     * @return 趋势点列表
     */
    List<WalletOverviewVO.TrendPoint> selectOverviewTrend(Long ownerId, Integer division, Long userId, Date start, Date end, String granularity);

    /**
     * 军团全量:取每个 division 最新 id 行的当前余额(income/expense 为 0)。
     *
     * @param ownerId 所有者ID(军团)
     * @return 各 division 最新余额(无数据分账不返回,服务层补零)
     */
    List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionBalances(Long ownerId, Long userId);

    /**
     * 军团全量:按 division 汇总区间收支(balance 为 0)。
     *
     * @param ownerId 所有者ID(军团)
     * @param start   起始时间(可空)
     * @param end     结束时间(可空)
     * @return 各 division 区间收入/支出
     */
    List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionFlow(Long ownerId, Long userId, Date start, Date end);
}