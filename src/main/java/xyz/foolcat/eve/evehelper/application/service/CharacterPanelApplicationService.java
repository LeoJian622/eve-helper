package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAttributesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterLocationInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterOnlineInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPanelInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPublicInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillList;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 角色面板应用服务(022 US1)。
 *
 * <p>提供三个用例:角色面板({@link #panel})、技能视图({@link #skills})、
 * 技能队列({@link #skillQueue})。每个用例的执行流程:
 * 属主校验 → 取 token → 组合网关查询 → 视图组装。</p>
 *
 * <p>关键设计:</p>
 * <ul>
 *     <li>缓存/强刷在网关层;应用层不做结果缓存(YAGNI)</li>
 *     <li>异常原样传播:authorize/getAccessTokenWithExpiry/网关异常不吞不包装</li>
 *     <li>舰船名解析:shipTypeId → InvTypesService.getNameByTypeIds;解析不到回 null</li>
 *     <li>技能队列:queueEndsAt = 末条 endDate(空列表 → null)</li>
 * </ul>
 *
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
public class CharacterPanelApplicationService {

    private final AuthorizeUtil authorizeUtil;
    private final EsiGateway esiGateway;
    private final InvTypesService invTypesService;

    /**
     * 角色面板:组合公开信息→在线→位置→舰船(网关层各自缓存,应用层不做结果缓存)。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 角色面板综合信息
     * @throws ParseException        JWT 解析失败(token 换取阶段)
     * @throws xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException 归属校验失败
     */
    public CharacterPanelInfo panel(Integer characterId, boolean forceRefresh) throws ParseException {
        // 1. 属主校验
        EveAccount account = authorizeUtil.authorize(characterId);

        // 2. 取 token
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        // 3. 组合顺序:公开信息 → 在线 → 位置 → 舰船
        CharacterPublicInfo publicInfo = esiGateway.queryCharacterPublicInfo(characterId, forceRefresh);
        CharacterOnlineInfo onlineInfo = esiGateway.queryCharacterOnline(characterId, accessToken, forceRefresh);
        CharacterLocationInfo locationInfo = esiGateway.queryCharacterLocation(characterId, accessToken, forceRefresh);
        Integer shipTypeId = esiGateway.queryCharacterShipTypeId(characterId, accessToken, forceRefresh);

        // 4. 舰船名解析(null 安全:解析不到回 null,不抛)
        String shipName = null;
        if (shipTypeId != null) {
            Map<Integer, String> nameMap = invTypesService.getNameByTypeIds(List.of(shipTypeId));
            shipName = nameMap.get(shipTypeId);
        }

        return new CharacterPanelInfo(
                publicInfo.characterId(),
                publicInfo.name(),
                publicInfo.corporationId(),
                publicInfo.allianceId(),
                publicInfo.birthday(),
                publicInfo.securityStatus(),
                onlineInfo.online(),
                onlineInfo.lastLogin(),
                onlineInfo.lastLogout(),
                locationInfo.solarSystemId(),
                locationInfo.stationId(),
                locationInfo.structureId(),
                shipTypeId,
                shipName
        );
    }

    /**
     * 技能视图:合并技能列表与属性。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 技能综合信息
     * @throws ParseException        JWT 解析失败
     * @throws xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException 归属校验失败
     */
    public CharacterSkillsInfo skills(Integer characterId, boolean forceRefresh) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        CharacterSkillList skillList = esiGateway.queryCharacterSkills(characterId, accessToken, forceRefresh);
        CharacterAttributesInfo attributes = esiGateway.queryCharacterAttributes(characterId, accessToken, forceRefresh);

        return new CharacterSkillsInfo(
                skillList.totalSp(),
                skillList.unallocatedSp(),
                attributes,
                skillList.skills()
        );
    }

    /**
     * 技能队列:空队列返回空列表与 null queueEndsAt(与查询失败可区分)。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 技能队列信息
     * @throws ParseException        JWT 解析失败
     * @throws xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException 归属校验失败
     */
    public SkillQueueInfo skillQueue(Integer characterId, boolean forceRefresh) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        List<SkillQueueItem> queue = esiGateway.queryCharacterSkillQueue(characterId, accessToken, forceRefresh);

        // queueEndsAt = 末条 endDate;空列表 → null
        OffsetDateTime queueEndsAt = null;
        if (!queue.isEmpty()) {
            queueEndsAt = queue.get(queue.size() - 1).endDate();
        }

        return new SkillQueueInfo(queue, queueEndsAt);
    }
}
