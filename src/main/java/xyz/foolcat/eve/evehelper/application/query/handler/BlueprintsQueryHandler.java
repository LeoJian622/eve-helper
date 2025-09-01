package xyz.foolcat.eve.evehelper.application.query.handler;

import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.application.query.model.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.annotation.QueryHandlers;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import javax.annotation.Resource;

/**
 * @author yongj
 * date 2025-08-12 17:07
 */

@QueryHandlers
public class BlueprintsQueryHandler implements QueryHandler<BlueprintsQuery, PageResult<BlueprintsDTO>> {

    @Resource
    private BlueprintsRepository blueprintsRepository;

    @Override
    public PageResult<BlueprintsDTO> handle(BlueprintsQuery query) {
        return blueprintsRepository.selectBlueprintsInvtypeUniverse(query, query.getOwnerId());
    }
}
