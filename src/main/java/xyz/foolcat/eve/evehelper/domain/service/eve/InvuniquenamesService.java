package xyz.foolcat.eve.evehelper.domain.service.eve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.Invuniquenames;
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

    public int updateBatch(List<Invuniquenames> list) {
        return invuniquenamesRepository.updateBatch(list);
    }

    public int batchInsert(List<Invuniquenames> list) {
        return invuniquenamesRepository.batchInsert(list);
    }

    public int insertOrUpdate(Invuniquenames record) {
        return invuniquenamesRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Invuniquenames record) {
        return invuniquenamesRepository.insertOrUpdateSelective(record);
    }
}
