package xyz.foolcat.eve.evehelper.domain.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;

import java.util.List;

public interface BlueprintsRepository {
    int updateBatch(List<Blueprints> list);

    int updateBatchSelective(List<Blueprints> list);

    int batchInsert(List<Blueprints> list);

    int insertOrUpdate(Blueprints record);

    int insertOrUpdateSelective(Blueprints record);

    IPage<BlueprintsVO> selectBlueprintsInvtypeUniverse(PageQuery page, String id);
} 