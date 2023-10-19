package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Asserts;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

@Mapper
public interface AssetsMapper extends BaseMapper<Asserts> {
    int updateBatch(List<Asserts> list);

    int updateBatchSelective(List<Asserts> list);

    int batchInsert(@Param("list") List<Asserts> list);

    int insertOrUpdate(Asserts record);

    int insertOrUpdateSelective(Asserts record);

    /**
     * 分页查询
     *
     * @param page
     * @param id
     * @return
     */
    IPage<AssetsVO> selectAssertsInvtypeUniverse(IPage<AssetsVO> page, @Param("id") String id);
}