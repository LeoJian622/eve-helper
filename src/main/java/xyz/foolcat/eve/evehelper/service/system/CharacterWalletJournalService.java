package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.CharacterWalletJournal;
import xyz.foolcat.eve.evehelper.mapper.system.CharacterWalletJournalMapper;

import java.util.List;

@Service
public class CharacterWalletJournalService extends ServiceImpl<CharacterWalletJournalMapper, CharacterWalletJournal> {


    public int batchInsert(List<CharacterWalletJournal> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(CharacterWalletJournal record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(CharacterWalletJournal record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

