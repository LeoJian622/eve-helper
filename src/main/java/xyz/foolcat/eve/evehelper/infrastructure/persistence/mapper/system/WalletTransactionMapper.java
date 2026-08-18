package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletTransactionPO;

/**
 * 钱包交易表(wallet_transaction)MyBatis Mapper。
 *
 * @author Leojan
 */
@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransactionPO> {

    /**
     * 按 (owner_type, owner_id, division) 物理分页查询钱包交易,时间 `date` 倒序。
     * date 为 MySQL 保留字,SQL 中须反引号。
     *
     * @param page      MyBatis Plus 分页参数,由分页插件注入 LIMIT
     * @param ownerType 所有者类型(character/corporation)
     * @param ownerId   所有者ID
     * @param division  钱包 division(人物=0,军团 1..7)
     * @return 分页结果(total 由分页插件填充)
     */
    IPage<WalletTransactionPO> selectPageByOwner(IPage<WalletTransactionPO> page,
                                                 @Param("ownerType") String ownerType,
                                                 @Param("ownerId") Long ownerId,
                                                 @Param("division") Integer division);

    /**
     * 依复合唯一键 UNIQUE(owner_type, owner_id, division, transaction_id)
     * 执行 {code insert ... on duplicate key update} 幂等 upsert。
     *
     * @param walletTransactionPO 待写入记录
     * @return 影响行数
     */
    int insertOrUpdateSelective(WalletTransactionPO walletTransactionPO);
}