package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;
import xyz.foolcat.eve.evehelper.domain.repository.system.EsiConfigRepository;

import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class EsiConfigService {

    private final EsiConfigRepository esiConfigRepository;

    public int updateBatch(List<EsiConfig> list) {
        return esiConfigRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<EsiConfig> list) {
        return esiConfigRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<EsiConfig> list) {
        return esiConfigRepository.batchInsert(list);
    }

    public int insertOrUpdate(EsiConfig record) {
        return esiConfigRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(EsiConfig record) {
        return esiConfigRepository.insertOrUpdateSelective(record);
    }
}

