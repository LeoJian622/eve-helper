package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse.IndustryJobItem;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.List;

/**
 * 工业直查应用服务(022 US3)。
 *
 * <p>提供直查 ESI 工业订单用例,不经 QQ 同步链路。
 * 支持角色(char)与军团(corp)两种类型,军团需 Director/Accountant 角色预检。</p>
 *
 * <p>关键设计:</p>
 * <ul>
 *     <li>includeCompleted=true 全量拉取,status 非 null 时内存精确过滤</li>
 *     <li>分页在内存切片(PageQuery current/size)</li>
 *     <li>corp 权限预检:queryCharacterRoles → hasIndustryPermission,false → permissionLow 透传</li>
 *     <li>异常原样传播:authorize/getAccessTokenWithExpiry/网关异常不吞不包装</li>
 * </ul>
 *
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
public class IndustryJobQueryApplicationService {

    /** type 为 char 时查询角色工业 */
    private static final String TYPE_CHAR = "char";

    /** type 为 corp 时查询军团工业 */
    private static final String TYPE_CORP = "corp";

    private final AuthorizeUtil authorizeUtil;
    private final EsiGateway esiGateway;

    /**
     * 直查工业订单。
     *
     * <p>流程:authorize → token → (corp 权限预检) → 网关全量拉取 → status 过滤 → 分页切片。</p>
     *
     * @param type         char/corp;非法值抛 PARAM_ERROR
     * @param characterId  角色 ID(corp 时 corpId 由 account.getCorpId() 派生)
     * @param status       状态过滤;null 时不过滤
     * @param pageQuery    分页参数(current/size)
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 分页响应
     * @throws ParseException        JWT 解析失败
     * @throws EveHelperException    归属校验失败或参数错误
     * @throws EsiAccessException   权限不足(corp 场景)
     */
    public IndustryJobPageResponse queryJobs(String type, Integer characterId, String status,
                                             PageQuery pageQuery, boolean forceRefresh) throws ParseException {
        // 1. 属主校验
        EveAccount account = authorizeUtil.authorize(characterId);

        // 2. 取 token
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        // 3. 网关拉取
        Flux<IndustryJob> jobFlux;
        if (TYPE_CHAR.equals(type)) {
            jobFlux = esiGateway.queryCharacterIndustryJobs(characterId, true, accessToken, forceRefresh);
        } else if (TYPE_CORP.equals(type)) {
            // corp 权限预检（MIN-A: roles 可 null，守卫对齐兄弟消费方）
            CharacterRolesInfo roles = esiGateway.queryCharacterRoles(characterId, accessToken);
            if (roles == null || !roles.hasIndustryPermission()) {
                throw EsiAccessException.permissionLow();
            }
            Integer corpId = account.getCorpId();
            jobFlux = esiGateway.queryCorporationIndustryJobs(corpId, true, accessToken, forceRefresh);
        } else {
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }

        // 4. 收集 → status 过滤
        List<IndustryJob> allJobs = jobFlux.collectList().block();
        List<IndustryJob> filtered = allJobs;
        if (status != null) {
            filtered = allJobs.stream()
                    .filter(j -> status.equals(j.getStatus()))
                    .toList();
        }

        // 5. 内存分页
        int total = filtered.size();
        int current = pageQuery.getCurrent();
        int size = pageQuery.getSize();
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<IndustryJobItem> pageItems;
        if (fromIndex >= total) {
            pageItems = List.of();
        } else {
            pageItems = filtered.subList(fromIndex, toIndex).stream()
                    .map(IndustryJobItem::from)
                    .toList();
        }

        return new IndustryJobPageResponse(pageItems, total);
    }
}
