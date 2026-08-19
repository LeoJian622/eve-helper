package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.query.StructurePageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureFuelDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureListItemDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureSummaryDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.StructurePoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.StructureMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class StructureRepositoryImpl implements StructureRepository {

    private final StructureMapper structureMapper;

    private final StructurePoConverter structurePoConverter;

    @Override
    public int updateBatch(List<Structure> list) {
        return structureMapper.updateBatch(structurePoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<Structure> list) {
        return structureMapper.updateBatchSelective(structurePoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<Structure> list) {
        return structureMapper.batchInsert(structurePoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(Structure record) {
        return structureMapper.insertOrUpdate(structurePoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Structure record) {
        return structureMapper.insertOrUpdateSelective(structurePoConverter.domain2Po(record));
    }

    @Override
    public Structure selectByStructureId(Long structureId) {
        return structurePoConverter.po2Domain(structureMapper.selectById(structureId));
    }

    @Override
    public List<Structure> selectFuelExpiresList(Integer hour, Integer corporationId) {
        return structurePoConverter.po2Domain(structureMapper.selectFuelExpiresList(hour, corporationId));
    }

    @Override
    public int removeBatchByIds(List<Long> ids) {
        return structureMapper.removeBatchByIds(ids);
    }

    @Override
    public List<Structure> selectByCorporationId(Integer corporationId) {
        return structurePoConverter.po2Domain(structureMapper.selectList(new QueryWrapper<StructurePO>().lambda().eq(StructurePO::getCorporationId, corporationId)));
    }

    @Override
    public int batchInsertOrUpdate(List<Structure> list) {
        return structureMapper.batchInsertOrUpdate(structurePoConverter.domain2Po(list));
    }

    @Override
    public PageResult<StructureListItemDTO> selectStructuresWithNames(StructurePageCriteria criteria, Long userId) {
        IPage<StructureListItemDTO> page = new Page<>(criteria.getCurrent(), criteria.getSize());
        StructurePageCriteria.SortField sortField = criteria.getSortField();
        page = structureMapper.selectStructuresWithNames(
                page,
                criteria.getCorporationId(),
                criteria.getName(),
                criteria.getState(),
                criteria.isLowFuelOnly(),
                sortField == null ? null : sortField.getColumn(),
                criteria.isAscending(),
                userId);
        return PageResult.<StructureListItemDTO>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .hasNext(page.getPages() - page.getCurrent() > 0)
                .hasPrevious(page.getCurrent() > 1)
                .build();
    }

    @Override
    public StructureDetailDTO selectDetailById(Long structureId, Long userId) {
        return structureMapper.selectDetailById(structureId, userId);
    }

    @Override
    public List<StructureFuelDTO> selectFuelExpiresListWithNames(String corporationId, Integer hour, Long userId) {
        return structureMapper.selectFuelExpiresListWithNames(corporationId, hour, userId);
    }

    @Override
    public StructureServiceDTO selectServicesById(Long structureId, Long userId) {
        return structureMapper.selectServicesById(structureId, userId);
    }

    @Override
    public List<StructureSummaryDTO> selectSummary(String corporationId, Long userId) {
        return structureMapper.selectSummary(corporationId, userId);
    }

    @Override
    public List<StructureTimerDTO> selectTimers(String corporationId, Long userId) {
        return structureMapper.selectTimers(corporationId, userId);
    }
}
