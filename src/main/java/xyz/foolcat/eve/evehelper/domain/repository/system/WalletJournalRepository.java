package xyz.foolcat.eve.evehelper.domain.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;

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
     * @return 领域实体分页结果
     */
    IPage<WalletJournal> selectPageByOwnerAndDivision(IPage<WalletJournal> page, Long ownerId, Integer division);
}