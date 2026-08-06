package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintsVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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

    /**
     * 允许的排序字段：请求参数名 -> 领域排序字段。
     * 白名单之外的输入一律拒绝，防止排序字段被拼入 SQL。
     */
    private static final Map<String, BlueprintsPageCriteria.SortField> SORT_FIELDS = Map.of(
            "typename", BlueprintsPageCriteria.SortField.TYPE_NAME,
            "materialefficiency", BlueprintsPageCriteria.SortField.MATERIAL_EFFICIENCY,
            "timeefficiency", BlueprintsPageCriteria.SortField.TIME_EFFICIENCY,
            "runs", BlueprintsPageCriteria.SortField.RUNS,
            "quantity", BlueprintsPageCriteria.SortField.QUANTITY,
            "itemid", BlueprintsPageCriteria.SortField.ITEM_ID);

    /**
     * 允许的蓝图类型：请求参数值 -> 领域筛选条件
     */
    private static final Map<String, BlueprintsPageCriteria.CopyFilter> COPY_FILTERS = Map.of(
            "original", BlueprintsPageCriteria.CopyFilter.ORIGINAL,
            "copy", BlueprintsPageCriteria.CopyFilter.COPY);

    /**
     * 所有者ID 必须为纯数字（库中为 BIGINT）
     */
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * 所有者ID 最大位数，BIGINT 上限为 19 位
     */
    private static final int OWNER_ID_MAX_LENGTH = 19;

    private final BlueprintsRepository blueprintsRepository;

    private final BlueprintsAssembler blueprintsAssembler;

    private final AccessGuard accessGuard;

    /**
     * 分页查询蓝图列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<BlueprintsVO> queryBlueprintsByPage(BlueprintsQuery query) {
        String ownerId = requireOwnerId(query.getOwnerId());
        // 归属校验先于其余参数解析，避免越权请求探测参数校验细节
        accessGuard.requireOwnership(ownerId, "蓝图清单");
        BlueprintsPageCriteria criteria = toCriteria(query, ownerId);
        return PageResultUtil.copy(
                blueprintsRepository.selectBlueprintsInvtypeUniverse(criteria),
                blueprintsAssembler::dto2Vo);
    }

    /**
     * 将请求 DTO 转换为领域查询条件，同时完成参数校验
     */
    private BlueprintsPageCriteria toCriteria(BlueprintsQuery query, String ownerId) {
        return BlueprintsPageCriteria.builder()
                .ownerId(ownerId)
                .blueprintName(normalizeBlueprintName(query.getBlueprintName()))
                .copyFilter(parseCopyFilter(query.getBlueprintType()))
                .sortField(parseSortField(query.getSortField()))
                .ascending(isAscending(query.getSortOrder()))
                .current(query.getCurrent())
                .size(query.getSize())
                .build();
    }

    private String requireOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new EveHelperException("所有者ID不能为空");
        }
        String trimmed = ownerId.trim();
        // owner_id 在库中为 BIGINT，非数字会下推数据库做隐式转换导致索引失效
        if (!DIGITS.matcher(trimmed).matches() || trimmed.length() > OWNER_ID_MAX_LENGTH) {
            throw new EveHelperException("所有者ID必须为不超过 " + OWNER_ID_MAX_LENGTH + " 位的数字：" + ownerId);
        }
        return trimmed;
    }

    /**
     * 空白名称归一为 null，避免生成无意义的全表 like
     */
    private String normalizeBlueprintName(String blueprintName) {
        if (blueprintName == null || blueprintName.isBlank()) {
            return null;
        }
        return blueprintName.trim();
    }

    private BlueprintsPageCriteria.CopyFilter parseCopyFilter(String blueprintType) {
        if (blueprintType == null || blueprintType.isBlank()) {
            return BlueprintsPageCriteria.CopyFilter.ANY;
        }
        BlueprintsPageCriteria.CopyFilter copyFilter =
                COPY_FILTERS.get(blueprintType.trim().toLowerCase(Locale.ROOT));
        if (copyFilter == null) {
            throw new EveHelperException("不支持的蓝图类型：" + blueprintType + "，可选值 original、copy");
        }
        return copyFilter;
    }

    private BlueprintsPageCriteria.SortField parseSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return null;
        }
        BlueprintsPageCriteria.SortField resolved =
                SORT_FIELDS.get(sortField.trim().toLowerCase(Locale.ROOT).replace("_", ""));
        if (resolved == null) {
            throw new EveHelperException("不支持的排序字段：" + sortField);
        }
        return resolved;
    }

    private boolean isAscending(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return false;
        }
        String normalized = sortOrder.trim().toLowerCase(Locale.ROOT);
        if ("asc".equals(normalized)) {
            return true;
        }
        if ("desc".equals(normalized)) {
            return false;
        }
        throw new EveHelperException("不支持的排序方向：" + sortOrder + "，可选值 asc、desc");
    }
}
