package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.query.handler.BlueprintsQueryHandler;
import xyz.foolcat.eve.evehelper.application.query.model.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.domain.service.system.BlueprintsService;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

/**
 * 蓝图应用服务
 * 负责协调领域服务和基础设施层，处理蓝图相关的业务用例
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlueprintsApplicationService {

    private final BlueprintsService blueprintsService;

    private final BlueprintsQueryHandler blueprintsQueryHandler;

    private final BlueprintsAssembler blueprintsAssembler;

    /**
     * 分页查询蓝图列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    public PageResult<BlueprintsVO> queryBlueprintsByPage(BlueprintsQuery queryDTO) {
        // 1. 参数验证（可以添加验证器）
        validateQueryParams(queryDTO);
        return PageResultUtil.copy(blueprintsQueryHandler.handle(queryDTO), blueprintsAssembler::dto2Vo);
    }

    /**
     * 验证查询参数
     */
    private void validateQueryParams(BlueprintsQuery queryDTO) {
        // 可以添加业务规则验证
        if (queryDTO.getOwnerId() == null || queryDTO.getOwnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("所有者ID不能为空");
        }

        // 可以添加权限验证
        // validatePermission(queryDTO.getOwnerId());
    }
} 