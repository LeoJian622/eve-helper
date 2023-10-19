package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Observer;

@Mapper
public interface ObserverMapper extends BaseMapper<Observer> {
    int updateBatch(List<Observer> list);

    int updateBatchSelective(List<Observer> list);

    int batchInsert(@Param("list") List<Observer> list);

    int insertOrUpdate(Observer record);

    int insertOrUpdateSelective(Observer record);
}