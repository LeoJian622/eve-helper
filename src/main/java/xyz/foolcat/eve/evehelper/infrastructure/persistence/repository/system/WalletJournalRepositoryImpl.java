package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewAggregate;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletJournalPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewAggregatePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewCategoryPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewDivisionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletOverviewTrendPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class WalletJournalRepositoryImpl implements WalletJournalRepository {

    private static final int BATCH_SIZE = 500;

    private final WalletJournalMapper walletJournalMapper;
    private final WalletJournalPoConverter walletJournalPoConverter;

    @Override
    public int updateBatch(List<WalletJournal> list) {
        return walletJournalMapper.updateBatch(walletJournalPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<WalletJournal> list) {
        return walletJournalMapper.updateBatchSelective(walletJournalPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<WalletJournal> list) {
        return walletJournalMapper.batchInsert(walletJournalPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(WalletJournal record) {
        return walletJournalMapper.insertOrUpdate(walletJournalPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(WalletJournal record) {
        return walletJournalMapper.insertOrUpdateSelective(walletJournalPoConverter.domain2Po(record));
    }

    @Override
    public void saveOrUpdateBatch(List<WalletJournal> walletJournals) {
        if (walletJournals == null || walletJournals.isEmpty()) {
            return;
        }
        List<WalletJournalPO> poList = new ArrayList<>(walletJournals.size());
        for (WalletJournal j : walletJournals) {
            poList.add(walletJournalPoConverter.domain2Po(j));
        }
        for (int i = 0; i < poList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, poList.size());
            walletJournalMapper.insertOrUpdateBatch(poList.subList(i, end));
        }
    }

    @Override
    public IPage<WalletJournal> selectPageByOwnerId(IPage<WalletJournal> page, Long ownerId) {
        // PO 分页内聚在本实现内:由领域实体维度 page 派生 PO 物理分页参数(current/size)
        IPage<WalletJournalPO> poPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<WalletJournalPO> poResult = walletJournalMapper.selectPageByOwnerId(poPage, ownerId);
        List<WalletJournal> domains = walletJournalPoConverter.po2Domain(poResult.getRecords());
        // 复用分页插件填充的总数(保留 size/current/total/pages),领域层不暴露 PO
        IPage<WalletJournal> result = new Page<>(poResult.getCurrent(), poResult.getSize(), poResult.getTotal());
        result.setRecords(domains);
        return result;
    }

    /**
     * 根据时间跨度计算角色某些类型的总税金额
     *
     * 税类型可以又 bounty_prizes，ess_escrow_transfer，corporate_reward_payout 等
     *
     * @param start 起始时间
     * @param end   结束时间
     * @param refType 记录类型集合
     * @return 角色->总额的key-value
     */
    @Override
    public IPage<WalletJournal> selectPageByOwnerAndDivision(IPage<WalletJournal> page, Long ownerId, Integer division, Long userId) {
        IPage<WalletJournalPO> poPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<WalletJournalPO> poResult = walletJournalMapper.selectPageByOwnerAndDivision(poPage, ownerId, division, userId);
        List<WalletJournal> domains = walletJournalPoConverter.po2Domain(poResult.getRecords());
        IPage<WalletJournal> result = new Page<>(poResult.getCurrent(), poResult.getSize(), poResult.getTotal());
        result.setRecords(domains);
        return result;
    }

    @Override
    public WalletOverviewAggregate selectOverviewAggregate(Long ownerId, Integer division, Long userId, Date start, Date end) {
        WalletOverviewAggregatePO po = walletJournalMapper.selectOverviewAggregate(ownerId, division, userId, start, end);
        if (po == null) {
            // 无任何流水行时聚合返回空,按契约回退为零值(金额/条数 0,时间 null)
            return new WalletOverviewAggregate(0.0, 0.0, 0.0, 0.0, 0L, null);
        }
        // WalletOverviewAggregate 组件序:(currentBalance, totalIncome, totalExpense, netFlow, journalCount, asOfTime)
        return new WalletOverviewAggregate(
                po.currentBalance() == null ? 0.0 : po.currentBalance(),
                zeroIfNull(po.totalIncome()),
                zeroIfNull(po.totalExpense()),
                zeroIfNull(po.netFlow()),
                zeroIfNull(po.journalCount()),
                po.asOfTime());
    }

    private Double zeroIfNull(Double v) {
        return v == null ? 0.0 : v;
    }

    private Long zeroIfNull(Long v) {
        return v == null ? 0L : v;
    }

    @Override
    public List<WalletOverviewVO.CategorySummary> selectOverviewCategories(Long ownerId, Integer division, Long userId, Date start, Date end) {
        List<WalletOverviewCategoryPO> pos = walletJournalMapper.selectOverviewCategories(ownerId, division, userId, start, end);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WalletOverviewVO.CategorySummary> result = new ArrayList<>(pos.size());
        for (WalletOverviewCategoryPO p : pos) {
            result.add(new WalletOverviewVO.CategorySummary(
                    p.refType(),
                    zeroIfNull(p.income()),
                    zeroIfNull(p.expense()),
                    p.count() == null ? 0L : p.count()));
        }
        return result;
    }

    @Override
    public List<WalletOverviewVO.TrendPoint> selectOverviewTrend(Long ownerId, Integer division, Long userId, Date start, Date end, String granularity) {
        List<WalletOverviewTrendPO> pos = walletJournalMapper.selectOverviewTrend(ownerId, division, userId, start, end, granularity);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WalletOverviewVO.TrendPoint> result = new ArrayList<>(pos.size());
        for (WalletOverviewTrendPO p : pos) {
            result.add(new WalletOverviewVO.TrendPoint(
                    p.bucket(),
                    zeroIfNull(p.income()),
                    zeroIfNull(p.expense()),
                    zeroIfNull(p.net())));
        }
        return result;
    }

    @Override
    public List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionBalances(Long ownerId, Long userId) {
        List<WalletOverviewDivisionPO> pos = walletJournalMapper.selectOverviewDivisionBalances(ownerId, userId);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WalletOverviewVO.DivisionSummary> result = new ArrayList<>(pos.size());
        for (WalletOverviewDivisionPO p : pos) {
            result.add(new WalletOverviewVO.DivisionSummary(
                    p.division(),
                    p.balance() == null ? 0.0 : p.balance(),
                    0.0,
                    0.0));
        }
        return result;
    }

    @Override
    public List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionFlow(Long ownerId, Long userId, Date start, Date end) {
        List<WalletOverviewDivisionPO> pos = walletJournalMapper.selectOverviewDivisionFlow(ownerId, userId, start, end);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WalletOverviewVO.DivisionSummary> result = new ArrayList<>(pos.size());
        for (WalletOverviewDivisionPO p : pos) {
            result.add(new WalletOverviewVO.DivisionSummary(
                    p.division(),
                    0.0,
                    p.income() == null ? 0.0 : p.income(),
                    p.expense() == null ? 0.0 : p.expense()));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> selectMapByDatetime(Date start, Date end, List<String> refType) {
        // character / date 均为 MySQL 保留字，列名必须加反引号；
        // 全部改用字符串列名而非 lambda 方法引用，避免 MyBatis-Plus 生成无反引号的 GROUP BY character 导致语法错误
        return walletJournalMapper.selectMaps(new QueryWrapper<WalletJournalPO>()
                .select("`character` as name,sum(amount) as amount")
                .in("ref_type", refType)
                .between("`date`", start, end)
                .groupBy("`character`"));

    }
} 