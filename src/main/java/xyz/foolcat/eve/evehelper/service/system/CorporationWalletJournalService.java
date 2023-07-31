package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.CorporationWalletJournal;
import xyz.foolcat.eve.evehelper.mapper.system.CorporationWalletJournalMapper;

import java.util.List;

@Service
public class CorporationWalletJournalService extends ServiceImpl<CorporationWalletJournalMapper, CorporationWalletJournal> {


    public int updateBatch(List<CorporationWalletJournal> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<CorporationWalletJournal> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<CorporationWalletJournal> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(CorporationWalletJournal record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(CorporationWalletJournal record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

