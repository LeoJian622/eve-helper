package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

/**
 * 蓝图应用服务
 * 负责蓝图相关的查询用例编排
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlueprintsApplicationService {

    private final BlueprintsRepository blueprintsRepository;

    private final BlueprintsAssembler blueprintsAssembler;

    /**
     * 分页查询蓝图列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<BlueprintsVO> queryBlueprintsByPage(BlueprintsQuery query) {
        validateQueryParams(query);
        return PageResultUtil.copy(
                blueprintsRepository.selectBlueprintsInvtypeUniverse(query, query.getOwnerId()),
                blueprintsAssembler::dto2Vo);
    }

    /**
     * 验证查询参数
     */
    private void validateQueryParams(BlueprintsQuery query) {
        if (query.getOwnerId() == null || query.getOwnerId().trim().isEmpty()) {
            throw new EveHelperException("所有者ID不能为空");
        }
    }
} 