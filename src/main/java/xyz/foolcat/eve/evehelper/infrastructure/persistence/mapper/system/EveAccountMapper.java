package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;

@Mapper
public interface EveAccountMapper extends BaseMapper<EveAccount> {
    // 只保留基础 CRUD
}
