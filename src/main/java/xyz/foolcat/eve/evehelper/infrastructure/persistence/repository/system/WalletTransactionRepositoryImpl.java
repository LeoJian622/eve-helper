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

import java.util.ArrayList;
import java.util.List;

/**
 * WalletTransaction 仓储实现。
 *
 * <p>幂等 upsert 由 mapper 的 {@code insert ... on duplicate key update} 承担,
 * 依复合键 {@code UNIQUE(owner_type, owner_id, division, transaction_id)}。
 * 批量写入(M4)按 {@link #BATCH_SIZE} 分批 flush,避免单条 SQL 超过
 * max_allowed_packet。分页(plan D4)PO 分页内聚在本实现,由领域维度 IPage
 * 派生物理分页参数,复用分页插件填充的 total。</p>
 *
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    /** 批量 upsert 每批行数(不超 MySQL max_allowed_packet) */
    private static final int BATCH_SIZE = 500;

    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletTransactionPoConverter walletTransactionPoConverter;

    @Override
    public void saveOrUpdateBatch(List<WalletTransaction> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 领域 → PO 转换
        List<WalletTransactionPO> poList = new ArrayList<>(list.size());
        for (WalletTransaction tx : list) {
            poList.add(walletTransactionPoConverter.domain2Po(tx));
        }
        // 分批 flush:每批 BATCH_SIZE 条,走 insertOrUpdateBatch(批量 INSERT ON DUPLICATE KEY UPDATE)
        for (int i = 0; i < poList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, poList.size());
            walletTransactionMapper.insertOrUpdateBatch(poList.subList(i, end));
        }
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