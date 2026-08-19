package xyz.foolcat.eve.evehelper.domain.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;

import java.util.List;

/**
 * 钱包交易(wallet_transaction)仓储接口。
 *
 * <p>幂等语义(plan D1):依复合唯一键 UNIQUE(owner_type, owner_id, division, transaction_id)
 * 做 upsert,同步重复调用不产生重复行。查询按 (owner_type, owner_id, division) 过滤,
 * 时间 `date` 倒序。</p>
 *
 * @author Leojan
 */
public interface WalletTransactionRepository {

    /**
     * 批量幂等 upsert:逐条走 insertOrUpdateSelective({@code insert ... on duplicate key update})。
     * 空集合直接返回,不做任何 DB 调用。
     *
     * @param list 待写入的交易记录
     */
    void saveOrUpdateBatch(List<WalletTransaction> list);

    /**
     * 按 (owner_type, owner_id, division) 分页查询钱包交易,时间 `date` 倒序。
     *
     * @param page      MyBatis Plus 分页参数(领域实体维度;PO 分页由实现内聚)
     * @param ownerType 所有者类型(character/corporation)
     * @param ownerId   所有者ID(Long,与库列 owner_id BIGINT 对齐)
     * @param division  钱包 division(人物=0,军团 1..7)
     * @param userId    军团维读过滤(US2b):null=ROOT/人物读不过滤;非 null(军团同步者)按 user_id 过滤
     * @return 领域实体分页结果(total 由分页插件填充)
     */
    IPage<WalletTransaction> selectPageByOwner(IPage<WalletTransaction> page,
                                               String ownerType, Long ownerId, Integer division, Long userId);
}