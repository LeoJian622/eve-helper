package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.esi.AssetsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.AssetsMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AssetsRepositoryImpl implements AssetsRepository {

    @Resource
    private AssetsMapper assetsMapper;

    @Resource
    private AssetsAssembler assetsAssembler;

    @Override
    public int updateBatch(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::entity2Po).collect(Collectors.toList());
        return assetsMapper.updateBatch(collect);
    }

    @Override
    public int updateBatchSelective(List<Assets> list) {

        List<AssetsPO> collect = list.stream().map(assetsAssembler::entity2Po).collect(Collectors.toList());
        return assetsMapper.updateBatchSelective(collect);
    }

    @Override
    public int batchInsert(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::entity2Po).collect(Collectors.toList());
        return assetsMapper.batchInsert(collect);
    }

    @Override
    public int insertOrUpdate(Assets record) {
        return assetsMapper.insertOrUpdate(assetsAssembler.entity2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Assets record) {
        return assetsMapper.insertOrUpdateSelective(assetsAssembler.entity2Po(record));
    }

    @Override
    public List<Assets> selectAssertsInvtypeUniverse(String id, int pages, int rows) {
        IPage<AssetsPO> page = new Page<>(pages, rows);
        return assetsMapper.selectAssertsInvtypeUniverse(page, id).stream()
                .map(assetsAssembler::po2Entity)
                .collect(Collectors.toList());
    }

    @Override
    public int batchInsertOrUpdate(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsAssembler::entity2Po).collect(Collectors.toList());
        return assetsMapper.batchInsertOrUpdate(collect);
    }

}