package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.BlueprintsPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class BlueprintsRepositoryImpl implements BlueprintsRepository {

    private final BlueprintsMapper blueprintsMapper;

    private final BlueprintsPoConverter blueprintsPoConverter;

    @Override
    public int updateBatch(List<Blueprints> list) {
        return blueprintsMapper.updateBatch(blueprintsPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<Blueprints> list) {
        return blueprintsMapper.updateBatchSelective(blueprintsPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<Blueprints> list) {
        return blueprintsMapper.batchInsert(blueprintsPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(Blueprints record) {
        return blueprintsMapper.insertOrUpdate(blueprintsPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Blueprints record) {
        return blueprintsMapper.insertOrUpdateSelective(blueprintsPoConverter.domain2Po(record));
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