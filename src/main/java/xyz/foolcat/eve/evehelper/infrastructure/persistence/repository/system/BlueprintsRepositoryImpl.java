package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper;
import xyz.foolcat.eve.evehelper.application.query.model.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class BlueprintsRepositoryImpl implements BlueprintsRepository {

    @Resource
    private BlueprintsMapper blueprintsMapper;

    @Resource
    private BlueprintsAssembler blueprintsAssembler;

    @Override
    public int updateBatch(List<Blueprints> list) {
        return blueprintsMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<Blueprints> list) {
        return blueprintsMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<Blueprints> list) {
        return blueprintsMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(Blueprints record) {
        return blueprintsMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(Blueprints record) {
        return blueprintsMapper.insertOrUpdateSelective(record);
    }

    @Override
    public PageResult<BlueprintsDTO> selectBlueprintsInvtypeUniverse(PageQuery page, String id) {
        IPage<BlueprintsDTO> blueprintsDTOPage = new Page<>();
        blueprintsDTOPage.setCurrent(page.getCurrent());
        blueprintsDTOPage.setSize(page.getSize());
        blueprintsDTOPage = blueprintsMapper.selectBlueprintsInvtypeUniverse(blueprintsDTOPage, id, page.getSortField(), page.getSortOrder());
        return PageResult.<BlueprintsDTO>builder()
                .records(blueprintsDTOPage.getRecords())
                .total(blueprintsDTOPage.getTotal())
                .current(blueprintsDTOPage.getCurrent())
                .size(blueprintsDTOPage.getSize())
                .pages(blueprintsDTOPage.getPages())
                .hasNext(blueprintsDTOPage.getPages() - blueprintsDTOPage.getCurrent() > 0)
                .hasPrevious(blueprintsDTOPage.getCurrent() > 1).build();
    }
}