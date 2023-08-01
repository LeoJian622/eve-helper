package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;

@Mapper
public interface EveCharacterMapper extends BaseMapper<EveCharacter> {
    int updateBatch(List<EveCharacter> list);

    int batchInsert(@Param("list") List<EveCharacter> list);

    int insertOrUpdate(EveCharacter record);

    int insertOrUpdateSelective(EveCharacter record);
}