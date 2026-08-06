package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.BlueprintsPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper;
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
    public PageResult<BlueprintsDTO> selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria criteria) {
        IPage<BlueprintsDTO> page = new Page<>(criteria.getCurrent(), criteria.getSize());
        BlueprintsPageCriteria.SortField sortField = criteria.getSortField();
        page = blueprintsMapper.selectBlueprintsInvtypeUniverse(
                page,
                criteria.getOwnerId(),
                criteria.getBlueprintName(),
                toIsBlueprintCopy(criteria.getCopyFilter()),
                sortField == null ? null : sortField.getColumn(),
                criteria.isAscending());
        return PageResult.<BlueprintsDTO>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .hasNext(page.getPages() - page.getCurrent() > 0)
                .hasPrevious(page.getCurrent() > 1).build();
    }

    /**
     * 将原图/拷贝筛选映射为数据库标志位，ANY 返回 null 表示不筛选
     */
    private Boolean toIsBlueprintCopy(BlueprintsPageCriteria.CopyFilter copyFilter) {
        return switch (copyFilter) {
            case COPY -> Boolean.TRUE;
            case ORIGINAL -> Boolean.FALSE;
            case ANY -> null;
        };
    }
}