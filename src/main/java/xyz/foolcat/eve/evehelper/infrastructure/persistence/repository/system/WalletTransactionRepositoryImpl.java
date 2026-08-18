package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletTransactionPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletTransactionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletTransactionMapper;

import java.util.List;

/**
 * WalletTransaction 仓储实现。
 *
 * <p>幂等 upsert(plan D1)由 mapper.insertOrUpdateSelective 承担,其 SQL 依复合键
 * {@code insert ... on duplicate key update}。分页(plan D4)PO 分页内聚在本实现,
 * 由领域维度 IPage 派生物理分页参数,复用分页插件填充的 total。</p>
 *
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletTransactionPoConverter walletTransactionPoConverter;

    @Override
    public void saveOrUpdateBatch(List<WalletTransaction> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 每条记录走 insertOrUpdateSelective(其 SQL 为 insert ... on duplicate key update,
        // 依复合键 UNIQUE(owner_type, owner_id, division, transaction_id) 幂等 upsert),
        // 同步重复调用不产生重复行;空实现会导致交易被静默丢弃
        list.forEach(transaction ->
                walletTransactionMapper.insertOrUpdateSelective(walletTransactionPoConverter.domain2Po(transaction)));
    }

    @Override
    public IPage<WalletTransaction> selectPageByOwner(IPage<WalletTransaction> page,
                                                      String ownerType, Long ownerId, Integer division) {
        // PO 分页内聚在本实现内:由领域实体维度 page 派生 PO 物理分页参数(current/size)
        IPage<WalletTransactionPO> poPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<WalletTransactionPO> poResult =
                walletTransactionMapper.selectPageByOwner(poPage, ownerType, ownerId, division);
        List<WalletTransaction> domains = walletTransactionPoConverter.po2Domain(poResult.getRecords());
        // 复用分页插件填充的总数(保留 size/current/total/pages),领域层不暴露 PO
        IPage<WalletTransaction> result =
                new Page<>(poResult.getCurrent(), poResult.getSize(), poResult.getTotal());
        result.setRecords(domains);
        return result;
    }
}