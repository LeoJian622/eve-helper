package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.AssetsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.AssetsMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class AssetsRepositoryImpl implements AssetsRepository {

    private final AssetsMapper assetsMapper;
    private final AssetsAssembler assetsAssembler;

    @Override
    public int updateBatch(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::domain2Po).collect(Collectors.toList());
        return assetsMapper.updateBatch(collect);
    }

    @Override
    public int updateBatchSelective(List<Assets> list) {

        List<AssetsPO> collect = list.stream().map(assetsAssembler::domain2Po).collect(Collectors.toList());
        return assetsMapper.updateBatchSelective(collect);
    }

    @Override
    public int batchInsert(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::domain2Po).collect(Collectors.toList());
        return assetsMapper.batchInsert(collect);
    }

    @Override
    public int insertOrUpdate(Assets record) {
        return assetsMapper.insertOrUpdate(assetsAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Assets record) {
        return assetsMapper.insertOrUpdateSelective(assetsAssembler.domain2Po(record));
    }

    @Override
    public List<Assets> selectAssertsInvtypeUniverse(String id, int pages, int rows) {
        IPage<AssetsPO> page = new Page<>(pages, rows);
        return assetsMapper.selectAssertsInvtypeUniverse(page, id).stream()
                .map(assetsAssembler::po2Domain)
                .collect(Collectors.toList());
    }

    @Override
    public int batchInsertOrUpdate(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::domain2Po).collect(Collectors.toList());
        return assetsMapper.batchInsertOrUpdate(collect);
    }

    @Override
    public void removeBatchByIds(List<Long> itemIds) {
        assetsMapper.removeByItemId(itemIds);
    }

    @Override
    public List<Assets> findByOwnerId(Integer characterId) {
        return assetsAssembler.po2Domain(assetsMapper.findByOwnerId(characterId));
    }
}