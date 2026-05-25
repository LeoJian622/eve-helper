package xyz.foolcat.eve.evehelper.application.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.AiQueryHistoryPO;

import java.util.List;

/**
 * AI查询历史对象转换器
 */
@Mapper(componentModel = "spring")
public interface AiQueryHistoryAssembler {

    AiQueryHistory po2Domain(AiQueryHistoryPO po);

    List<AiQueryHistory> poList2DomainList(List<AiQueryHistoryPO> poList);

    AiQueryHistoryPO domain2Po(AiQueryHistory domain);
}
