package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;

import java.util.List;

@Mapper
public interface EveAccountMapper extends BaseMapper<EveAccountPO> {

    int updateBatch(List<EveAccountPO> list);

    int insertOrUpdateSelective(EveAccountPO record);

    int updateBatchSelective(List<EveAccountPO> list);

    int batchInsert(List<EveAccountPO> list);

    EveAccountPO queryOneUserIdAndCharacterId(@Param("userId") Integer userId,@Param("cId") Integer cId);

    List<EveAccountPO> queryAccountList(Integer userId);

    // 只保留基础 CRUD
}
