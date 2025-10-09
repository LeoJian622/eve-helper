package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.UniverseNameAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.repository.system.UniverseNameRepository;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class UniverseNameService  {

    private final UniverseNameAssembler universeNameAssembler;

    private final UniverseApi universeApi;

    private final UniverseNameRepository universeNameRepository;

    public int updateBatch(List<UniverseName> list) {
        return universeNameRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<UniverseName> list) {
        return universeNameRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<UniverseName> list) {
        return universeNameRepository.batchInsert(list);
    }

    public Map<Integer, String> getUniverseName(List<Integer> items) {
        List<UniverseName> database = universeNameRepository.selectByIdIn(items);

        List<Integer> inItems = database.stream().map(universeName -> universeName.getId().intValue()).collect(Collectors.toList());

        List<Integer> noInItems = items.stream().filter(item -> !inItems.contains(item)).collect(Collectors.toList());

        List<UniverseName> newUnivereName = new ArrayList<>();
        if (!noInItems.isEmpty()) {
            List<Id2NameResponse> nameResponses = universeApi.queryUniverseNames(noInItems, EsiClient.SERENITY).collectList().block();
            assert nameResponses != null;
            newUnivereName = nameResponses.stream().map(universeNameAssembler::id2NameResponse2UniverseName).collect(Collectors.toList());
            universeNameRepository.saveOrUpdateBatch(newUnivereName);

        }

        database.addAll(newUnivereName);

        return database.stream().collect(Collectors.toMap(UniverseName::getId, UniverseName::getName));

    }

    public int insertOrUpdate(UniverseName record) {
        return universeNameRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(UniverseName record) {
        return universeNameRepository.insertOrUpdateSelective(record);
    }
}


