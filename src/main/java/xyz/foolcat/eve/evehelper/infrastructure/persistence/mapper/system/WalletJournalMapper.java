package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewAggregatePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewCategoryPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewDivisionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewTrendPO;

import java.util.Date;
import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface WalletJournalMapper extends BaseMapper<WalletJournalPO> {

    /**
     * 按所有者ID物理分页查询钱包流水(index:selectPageByOwnerId,时间 date 倒序)。
     *
     * @param page    MyBatis Plus 分页参数,由分页插件注入 LIMIT
     * @param ownerId 所有者ID(人物ID 或军团ID)
     * @return 分页结果(total 由分页插件填充)
     */
    IPage<WalletJournalPO> selectPageByOwnerId(IPage<WalletJournalPO> page, @Param("ownerId") Long ownerId);

    int updateBatch(List<WalletJournalPO> walletJournalPOS);

    int updateBatchSelective(List<WalletJournalPO> walletJournalPOS);

    int batchInsert(List<WalletJournalPO> walletJournalPOS);

    int insertOrUpdateSelective(WalletJournalPO walletJournalPO);

    /**
     * 按所有者ID与分账物理分页查询钱包流水(时间 date 倒序)。
     *
     * @param page     MyBatis Plus 分页参数
     * @param ownerId  所有者ID
     * @param division 分账
     * @return 分页结果
     */
    IPage<WalletJournalPO> selectPageByOwnerAndDivision(IPage<WalletJournalPO> page, @Param("ownerId") Long ownerId, @Param("division") Integer division);

    void insertOrUpdateBatch(@Param("list") List<WalletJournalPO> list);

    /**
     * 钱包总览标量聚合(收支/净流/条数/当前余额/统计截点)。
     *
     * @param ownerId  所有者ID(人物或军团)
     * @param division 分账;人物传 null 不过滤(兼容存量 division NULL/1)
     * @param start    起始时间(可空,不过滤)
     * @param end      结束时间(可空,不过滤)
     * @return 标量聚合
     */
    WalletOverviewAggregatePO selectOverviewAggregate(@Param("ownerId") Long ownerId, @Param("division") Integer division,
                                                      @Param("start") Date start, @Param("end") Date end);

    /**
     * 钱包总览按交易类型(ref_type)的收支/条数汇总,按 (income+expense) 降序,最多 50 笔类型。
     *
     * @param ownerId  所有者ID
     * @param division 分账;人物传 null 不过滤
     * @param start    起始时间(可空)
     * @param end      结束时间(可空)
     * @return 类型汇总列表
     */
    List<WalletOverviewCategoryPO> selectOverviewCategories(@Param("ownerId") Long ownerId, @Param("division") Integer division,
                                                            @Param("start") Date start, @Param("end") Date end);

    /**
     * 钱包总览按时间桶的趋势(收支/净额),按桶升序。
     *
     * @param ownerId     所有者ID
     * @param division    分账;人物传 null 不过滤
     * @param start       起始时间(可空)
     * @param end         结束时间(可空)
     * @param granularity 时间桶格式(如 %Y-%m 月粒度 / %Y-%m-%d 日粒度)
     * @return 趋势点列表
     */
    List<WalletOverviewTrendPO> selectOverviewTrend(@Param("ownerId") Long ownerId, @Param("division") Integer division,
                                                    @Param("start") Date start, @Param("end") Date end,
                                                    @Param("granularity") String granularity);

    /**
     * 军团全量:取每个 division 最新 id 行的当前余额。
     *
     * @param ownerId 所有者ID(军团)
     * @return 各 division 最新余额(无数据的分账不返回,交由服务层补零)
     */
    List<WalletOverviewDivisionPO> selectOverviewDivisionBalances(@Param("ownerId") Long ownerId);

    /**
     * 军团全量:按 division 汇总区间收支(时间过滤作用于此)。
     *
     * @param ownerId 所有者ID(军团)
     * @param start   起始时间(可空,不过滤)
     * @param end     结束时间(可空,不过滤)
     * @return 各 division 区间收入/支出(无数据分账不返回)
     */
    List<WalletOverviewDivisionPO> selectOverviewDivisionFlow(@Param("ownerId") Long ownerId,
                                                              @Param("start") Date start, @Param("end") Date end);
}
