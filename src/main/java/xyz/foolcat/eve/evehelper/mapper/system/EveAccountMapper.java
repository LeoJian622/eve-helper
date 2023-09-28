package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;

import java.util.List;

@Mapper
public interface EveAccountMapper extends BaseMapper<EveAccount> {
    int updateBatch(List<EveAccount> list);

    int batchInsert(@Param("list") List<EveAccount> list);

    int insertOrUpdate(EveAccount record);

    int insertOrUpdateSelective(EveAccount record);
}