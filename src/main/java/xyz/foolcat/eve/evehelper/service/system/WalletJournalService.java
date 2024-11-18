package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.WalletJournal;
import xyz.foolcat.eve.evehelper.mapper.system.WalletJournalMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class WalletJournalService extends ServiceImpl<WalletJournalMapper, WalletJournal> {

    public int batchInsert(List<WalletJournal> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(WalletJournal record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(WalletJournal record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    public int updateBatch(List<WalletJournal> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<WalletJournal> list) {
        return baseMapper.updateBatchSelective(list);
    }
}


