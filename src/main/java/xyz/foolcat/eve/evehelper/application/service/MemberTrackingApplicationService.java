package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingItem;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 团员追踪应用服务(T038, US5 GREEN)。
 * <p>
 * 协调领域端口完成团员追踪用例:归属校验 → 取 token → Director 角色校验
 * → 网关直查 → online 派生 → 返回聚合 VO。
 *
 * @author Leojian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTrackingApplicationService {

    private final EsiGateway esiGateway;
    private final AuthorizeUtil authorizeUtil;

    /**
     * 追踪军团成员在线状态。
     *
     * @param corpId       军团 ID
     * @param characterId  人物 ID(必须属于当前用户且具 Director 角色)
     * @param forceRefresh true 强制刷新绕过缓存
     * @return 团员追踪聚合 VO
     * @throws ParseException JWT 解析失败
     */
    public MemberTrackingInfo track(Integer corpId, Integer characterId, boolean forceRefresh) throws ParseException {
        // 1. 归属前置:fail-closed
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }

        // 2. 归属校验(他人角色由 AuthorizeUtil 抛 ACCESS_UNAUTHORIZED)
        EveAccount account = authorizeUtil.authorize(characterId);

        // 2b. MIN-E: 跨军团本地守卫（镜像 IndustryJobQuery:85 从 account.getCorpId 派生）
        if (!corpId.equals(account.getCorpId())) {
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }

        // 3. 取 ESI access token
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String token = tokenResult.accessToken();

        // 4. Director 角色校验
        CharacterRolesInfo roles = esiGateway.queryCharacterRoles(characterId, token);
        if (roles == null || !roles.hasDirectorRole()) {
            throw EsiAccessException.permissionLow();
        }

        // 5. 网关直查
        List<MemberTrackingItem> items = esiGateway.queryCorporationMemberTracking(corpId, token, forceRefresh);

        // 6. 映射 + online 派生
        List<MemberTrackingInfo.MemberTrackingMember> members = items.stream()
                .map(item -> new MemberTrackingInfo.MemberTrackingMember(
                        item.characterId(),
                        item.logonDate() != null && (item.logoffDate() == null || item.logonDate().isAfter(item.logoffDate())),
                        item.locationId(),
                        item.shipTypeId(),
                        item.logonDate(),
                        item.logoffDate()
                ))
                .toList();

        return new MemberTrackingInfo(members, OffsetDateTime.now());
    }
}
