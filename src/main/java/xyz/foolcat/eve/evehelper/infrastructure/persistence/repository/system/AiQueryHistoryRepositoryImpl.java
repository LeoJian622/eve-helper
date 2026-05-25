package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.AiQueryHistoryAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;
import xyz.foolcat.eve.evehelper.domain.repository.system.AiQueryHistoryRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.AiQueryHistoryPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.AiQueryHistoryMapper;

import java.util.List;

/**
 * AI查询历史仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AiQueryHistoryRepositoryImpl implements AiQueryHistoryRepository {

    private final AiQueryHistoryMapper mapper;
    private final AiQueryHistoryAssembler assembler;

    @Override
    public AiQueryHistory save(AiQueryHistory history) {
        AiQueryHistoryPO po = assembler.domain2Po(history);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return assembler.po2Domain(po);
    }

    @Override
    public List<AiQueryHistory> findByUserId(Integer userId, int limit) {
        List<AiQueryHistoryPO> poList = mapper.findByUserId(userId, limit);
        return assembler.poList2DomainList(poList);
    }

    @Override
    public AiQueryHistory findById(Long id) {
        AiQueryHistoryPO po = mapper.selectById(id);
        return po != null ? assembler.po2Domain(po) : null;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
