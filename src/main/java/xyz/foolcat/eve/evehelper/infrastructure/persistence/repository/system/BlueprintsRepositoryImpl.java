package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;
import java.util.List;

@Repository
public class BlueprintsRepositoryImpl implements BlueprintsRepository {
    @Autowired
    private BlueprintsMapper blueprintsMapper;

    @Override
    public int updateBatch(List<BlueprintsPO> list) {
        return blueprintsMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<BlueprintsPO> list) {
        return blueprintsMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<BlueprintsPO> list) {
        return blueprintsMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(BlueprintsPO record) {
        return blueprintsMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(BlueprintsPO record) {
        return blueprintsMapper.insertOrUpdateSelective(record);
    }

    @Override
    public IPage<BlueprintsVO> selectBlueprintsInvtypeUniverse(IPage<BlueprintsVO> page, String id) {
        return blueprintsMapper.selectBlueprintsInvtypeUniverse(page, id);
    }
} 