package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.vo.BlueprintsVO;

@Mapper
public interface BlueprintsMapper extends BaseMapper<Blueprints> {
    int batchInsert(@Param("list") List<Blueprints> list);

    /**
     * 分页查询
     * @param page
     * @param id
     * @return
     */
    IPage<BlueprintsVO> selectBlueprintsInvtypeUniverse(IPage<BlueprintsVO> page, @Param("id") String id);
}