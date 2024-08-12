package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.converter.UniverseNameConverter;
import xyz.foolcat.eve.evehelper.domain.system.UniverseName;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.mapper.system.UniverseNameMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class UniverseNameService extends ServiceImpl<UniverseNameMapper, UniverseName> {

    private final UniverseNameConverter universeNameConverter;
    private final UniverseApi universeApi;

    public int updateBatch(List<UniverseName> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<UniverseName> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<UniverseName> list) {
        return baseMapper.batchInsert(list);
    }

    public Map<Integer, String> getUniverseName(List<Integer> items) {
        List<UniverseName> database = this.lambdaQuery().in(UniverseName::getId, items).list();

        List<Integer> inItems = database.stream().map(universeName -> universeName.getId().intValue()).collect(Collectors.toList());

        List<Integer> noInItems = items.stream().filter(item -> !inItems.contains(item)).collect(Collectors.toList());

        List<UniverseName> newUnivereName = new ArrayList<>();
        if (!noInItems.isEmpty()) {
            List<Id2NameResponse> nameResponses = universeApi.queryUniverseNames(noInItems, EsiClient.SERENITY).collectList().block();
            assert nameResponses != null;
            newUnivereName = nameResponses.stream().map(universeNameConverter::id2NameResponse2UniverseName).collect(Collectors.toList());
            saveBatch(newUnivereName);

        }

        database.addAll(newUnivereName);

        Map<Integer, String> universeNameMap = database.stream().collect(Collectors.toMap(UniverseName::getId, UniverseName::getName));

        return universeNameMap;

    }

    public int insertOrUpdate(UniverseName record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(UniverseName record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}


