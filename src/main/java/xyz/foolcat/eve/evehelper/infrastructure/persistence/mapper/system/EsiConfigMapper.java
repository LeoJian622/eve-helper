package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EsiConfigPO;

@Mapper
public interface EsiConfigMapper extends BaseMapper<EsiConfigPO> {
    // 只保留基础 CRUD
}
