package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

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
}
