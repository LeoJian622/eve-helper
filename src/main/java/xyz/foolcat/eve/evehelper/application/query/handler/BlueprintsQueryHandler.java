package xyz.foolcat.eve.evehelper.application.query.handler;

import lombok.RequiredArgsConstructor;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.application.query.model.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.annotation.QueryHandlers;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

/**
 * @author yongj
 * date 2025-08-12 17:07
 */

@QueryHandlers
@RequiredArgsConstructor
public class BlueprintsQueryHandler implements QueryHandler<BlueprintsQuery, PageResult<BlueprintsDTO>> {

    private final BlueprintsRepository blueprintsRepository;

    @Override
    public PageResult<BlueprintsDTO> handle(BlueprintsQuery query) {
        return blueprintsRepository.selectBlueprintsInvtypeUniverse(query, query.getOwnerId());
    }
}
