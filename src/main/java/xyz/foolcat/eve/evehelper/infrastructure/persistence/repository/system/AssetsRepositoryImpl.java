package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.AssetsPoConverter;
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
    private final AssetsPoConverter assetsPoConverter;

    @Override
    public int updateBatch(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsPoConverter::domain2Po).collect(Collectors.toList());
        return assetsMapper.updateBatch(collect);
    }

    @Override
    public int updateBatchSelective(List<Assets> list) {

        List<AssetsPO> collect = list.stream().map(assetsPoConverter::domain2Po).collect(Collectors.toList());
        return assetsMapper.updateBatchSelective(collect);
    }

    @Override
    public int batchInsert(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsPoConverter::domain2Po).collect(Collectors.toList());
        return assetsMapper.batchInsert(collect);
    }

    @Override
    public boolean insertOrUpdate(Assets record) {
        return assetsMapper.insertOrUpdate(assetsPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Assets record) {
        return assetsMapper.insertOrUpdateSelective(assetsPoConverter.domain2Po(record));
    }

    @Override
    public List<Assets> selectAssertsInvtypeUniverse(String id, int pages, int rows) {
        IPage<AssetsPO> page = new Page<>(pages, rows);
        return assetsMapper.selectAssertsInvtypeUniverse(page, id).stream()
                .map(assetsPoConverter::po2Domain)
                .collect(Collectors.toList());
    }

    @Override
    public int batchInsertOrUpdate(List<Assets> list) {
        List<AssetsPO> collect = list.stream().map(assetsPoConverter::domain2Po).collect(Collectors.toList());
        return assetsMapper.batchInsertOrUpdate(collect);
    }

    @Override
    public void removeBatchByIds(List<Long> itemIds) {
        // 空列表时 foreach 展开为空导致 in 后无内容，触发 SQL 语法错误/Druid wall 拦截；空列表 = 无待删项，跳过
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        assetsMapper.removeByItemId(itemIds);
    }

    @Override
    public List<Assets> findByOwnerId(Integer characterId) {
        return assetsPoConverter.po2Domain(assetsMapper.findByOwnerId(characterId));
    }
}
