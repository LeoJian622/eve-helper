package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.CharacterWalletJournal;

@Mapper
public interface CharacterWalletJournalMapper extends BaseMapper<CharacterWalletJournal> {
    int batchInsert(@Param("list") List<CharacterWalletJournal> list);

    int insertOrUpdate(CharacterWalletJournal record);

    int insertOrUpdateSelective(CharacterWalletJournal record);
}