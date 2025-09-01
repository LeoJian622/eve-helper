package xyz.foolcat.eve.evehelper.domain.service.eve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.domain.repository.eve.InvuniquenamesRepository;

import java.util.List;

/**
 * @author yongj
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class InvuniquenamesService {

    private final InvuniquenamesRepository invuniquenamesRepository;

    public int updateBatch(List<InvUniqueNames> list) {
        return invuniquenamesRepository.updateBatch(list);
    }

    public int batchInsert(List<InvUniqueNames> list) {
        return invuniquenamesRepository.batchInsert(list);
    }

    public int insertOrUpdate(InvUniqueNames record) {
        return invuniquenamesRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(InvUniqueNames record) {
        return invuniquenamesRepository.insertOrUpdateSelective(record);
    }
}
