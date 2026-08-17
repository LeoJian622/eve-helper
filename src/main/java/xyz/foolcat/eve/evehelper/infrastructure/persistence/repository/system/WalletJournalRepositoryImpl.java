package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletJournalPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class WalletJournalRepositoryImpl implements WalletJournalRepository {

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
        // 每条记录走 insertOrUpdateSelective(其 SQL 为 insert ... on duplicate key update,
        // 幂等 upsert):WalletJournalService 同步 ESI 钱包日志时经此落库,空实现会导致日志被静默丢弃
        walletJournals.forEach(walletJournal ->
                walletJournalMapper.insertOrUpdateSelective(walletJournalPoConverter.domain2Po(walletJournal)));
    }

    @Override
    public IPage<WalletJournal> selectPageByOwnerId(IPage<WalletJournal> page, Integer ownerId) {
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