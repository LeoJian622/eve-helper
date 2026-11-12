package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CharacterPanelApplicationService 单元测试（TDD RED, 022-T007）。
 *
 * <p>纯 Mockito 单测,固化 US1 角色面板应用服务的 6 个行为契约。
 * 本任务仅写失败测试——CharacterPanelApplicationService 与三个视图 VO
 * (CharacterPanelInfo/CharacterSkillsInfo/SkillQueueInfo) 由 T012 创建,
 * 编译失败本身即 RED 证据。</p>
 *
 * <p>用例:
 * <ol>
 *   <li>panel_composesFourGatewayCalls_andResolvesShipName</li>
 *   <li>skills_mergesSkillListAndAttributes</li>
 *   <li>skillQueue_emptyQueue_returnsEmptyListWithNullQueueEndsAt</li>
 *   <li>skillQueue_queueEndsAt_equalsLastEntryEndDate</li>
 *   <li>panel_authFailure_propagatesDistinguishableError (US1-4)</li>
 *   <li>panel_otherUsersCharacter_rejected (US1-5)</li>
 * </ol>
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterPanelApplicationService 角色面板应用服务(022 US1)")
class CharacterPanelApplicationServiceTest {

    private static final int CHARACTER_ID = 95465499;
    private static final int USER_ID = 1;
    private static final int CORP_ID = 98000001;
    private static final int ALLIANCE_ID = 99000001;
    private static final int SOLAR_SYSTEM_ID = 30002187;
    private static final int STATION_ID = 60003760;
    private static final Long STRUCTURE_ID = null;
    private static final int SHIP_TYPE_ID = 670;
    private static final String SHIP_NAME = "Gila";
    private static final String STUB_ACCESS_TOKEN = "Bearer stub-token-for-test";

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private InvTypesService invTypesService;

    // ── 辅助方法 ──

    private EveAccount buildAccount() {
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setCharacterName("TestChar");
        account.setCorpId(CORP_ID);
        account.setUserId(USER_ID);
        return account;
    }

    private void stubAuthorize() {
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(buildAccount());
    }

    private void stubAccessToken() throws ParseException {
        when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID))
                .thenReturn(new CharacterAccessTokenResult(STUB_ACCESS_TOKEN, CHARACTER_ID, 1140L));
    }

    /* ============================ US1-1 角色面板 ============================ */

    @Test
    @DisplayName("panel 组合四网关调用并解析舰船名")
    void panel_composesFourGatewayCalls_andResolvesShipName() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        OffsetDateTime birthday = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        when(esiGateway.queryCharacterPublicInfo(eq(CHARACTER_ID), any(Boolean.class)))
                .thenReturn(new CharacterPublicInfo(CHARACTER_ID, "TestChar", CORP_ID, ALLIANCE_ID, birthday, 2.5));

        OffsetDateTime lastLogin = OffsetDateTime.parse("2026-11-12T10:00:00Z");
        OffsetDateTime lastLogout = OffsetDateTime.parse("2026-11-12T09:00:00Z");
        when(esiGateway.queryCharacterOnline(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(new CharacterOnlineInfo(true, lastLogin, lastLogout));

        when(esiGateway.queryCharacterLocation(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(new CharacterLocationInfo(SOLAR_SYSTEM_ID, STATION_ID, STRUCTURE_ID));

        when(esiGateway.queryCharacterShipTypeId(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(SHIP_TYPE_ID);

        when(invTypesService.getNameByTypeIds(List.of(SHIP_TYPE_ID)))
                .thenReturn(Map.of(SHIP_TYPE_ID, SHIP_NAME));

        // 服务与 CharacterPanelInfo VO 由 T012 创建;此处构造器顺序已固化
        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        CharacterPanelInfo panel = service.panel(CHARACTER_ID, false);

        assertThat(panel.characterId()).isEqualTo(CHARACTER_ID);
        assertThat(panel.name()).isEqualTo("TestChar");
        assertThat(panel.corporationId()).isEqualTo(CORP_ID);
        assertThat(panel.allianceId()).isEqualTo(ALLIANCE_ID);
        assertThat(panel.birthday()).isEqualTo(birthday);
        assertThat(panel.securityStatus()).isEqualTo(2.5);
        assertThat(panel.online()).isTrue();
        assertThat(panel.lastLogin()).isEqualTo(lastLogin);
        assertThat(panel.lastLogout()).isEqualTo(lastLogout);
        assertThat(panel.solarSystemId()).isEqualTo(SOLAR_SYSTEM_ID);
        assertThat(panel.stationId()).isEqualTo(STATION_ID);
        assertThat(panel.structureId()).isEqualTo(STRUCTURE_ID);
        assertThat(panel.shipTypeId()).isEqualTo(SHIP_TYPE_ID);
        assertThat(panel.shipName()).isEqualTo(SHIP_NAME);

        // verify 四网关调用各一次(forceRefresh 透传 false)
        verify(esiGateway).queryCharacterPublicInfo(CHARACTER_ID, false);
        verify(esiGateway).queryCharacterOnline(CHARACTER_ID, STUB_ACCESS_TOKEN, false);
        verify(esiGateway).queryCharacterLocation(CHARACTER_ID, STUB_ACCESS_TOKEN, false);
        verify(esiGateway).queryCharacterShipTypeId(CHARACTER_ID, STUB_ACCESS_TOKEN, false);
    }

    /* ============================ US1-2 技能视图 ============================ */

    @Test
    @DisplayName("skills 合并技能列表与属性")
    void skills_mergesSkillListAndAttributes() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        List<CharacterSkillList.SkillItem> skillItems = List.of(
                new CharacterSkillList.SkillItem(3300, 5, 5, 256000L)
        );
        when(esiGateway.queryCharacterSkills(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(new CharacterSkillList(50000000L, 100000, skillItems));

        CharacterAttributesInfo attrs = new CharacterAttributesInfo(25, 22, 20, 23, 24, 2);
        when(esiGateway.queryCharacterAttributes(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(attrs);

        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        CharacterSkillsInfo skills = service.skills(CHARACTER_ID, false);

        assertThat(skills.totalSp()).isEqualTo(50000000L);
        assertThat(skills.unallocatedSp()).isEqualTo(100000);
        assertThat(skills.attributes()).isEqualTo(attrs);
        assertThat(skills.skills()).hasSize(1);
        assertThat(skills.skills().get(0).skillId()).isEqualTo(3300);
    }

    /* ============================ US1-3 技能队列 ============================ */

    @Test
    @DisplayName("skillQueue 空队列返回空列表与 null queueEndsAt(与查询失败可区分)")
    void skillQueue_emptyQueue_returnsEmptyListWithNullQueueEndsAt() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.queryCharacterSkillQueue(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(List.of());

        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        SkillQueueInfo queue = service.skillQueue(CHARACTER_ID, false);

        // 空队列 ≠ 查询失败:queue 为空列表,queueEndsAt 为 null,方法正常返回不抛异常
        assertThat(queue.queue()).isEmpty();
        assertThat(queue.queueEndsAt()).isNull();
    }

    @Test
    @DisplayName("skillQueue queueEndsAt 等于末条 endDate")
    void skillQueue_queueEndsAt_equalsLastEntryEndDate() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        OffsetDateTime firstEnd = OffsetDateTime.parse("2026-11-13T08:00:00Z");
        OffsetDateTime lastEnd = OffsetDateTime.parse("2026-11-15T20:00:00Z");
        List<SkillQueueItem> items = List.of(
                new SkillQueueItem(3300, 5, 0L, 256000L,
                        OffsetDateTime.parse("2026-11-12T10:00:00Z"), firstEnd, 0),
                new SkillQueueItem(3301, 4, 256000L, 512000L, firstEnd, lastEnd, 1)
        );
        when(esiGateway.queryCharacterSkillQueue(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), any(Boolean.class)))
                .thenReturn(items);

        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        SkillQueueInfo queue = service.skillQueue(CHARACTER_ID, false);

        assertThat(queue.queue()).hasSize(2);
        assertThat(queue.queueEndsAt()).isEqualTo(lastEnd);
    }

    /* ============================ US1-4 ESI 授权失败传播 ============================ */

    @Test
    @DisplayName("panel ESI 授权失败传播可区分异常(不吞、不返回半成品)")
    void panel_authFailure_propagatesDistinguishableError() throws ParseException {
        stubAuthorize();
        // ESI 授权失效:token 换取抛 ParseException(JWT 解析失败)
        when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID))
                .thenThrow(new ParseException("ESI token expired or invalid", 0));

        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        // 断言服务传播异常,不吞掉也不返回半成品 panel
        assertThatThrownBy(() -> service.panel(CHARACTER_ID, false))
                .isInstanceOf(Exception.class);

        // 网关查询不应发生(token 换取已失败)
        verify(esiGateway, never()).queryCharacterPublicInfo(anyInt(), any(Boolean.class));
        verify(esiGateway, never()).queryCharacterOnline(anyInt(), any(), any(Boolean.class));
    }

    /* ============================ US1-5 越权拒绝 ============================ */

    @Test
    @DisplayName("panel 其他用户角色被拒绝(AuthorizeUtil 抛 ACCESS_UNAUTHORIZED)")
    void panel_otherUsersCharacter_rejected() {
        // AuthorizeUtil 检测到越权,抛出 EveHelperException(ACCESS_UNAUTHORIZED)
        when(authorizeUtil.authorize(CHARACTER_ID))
                .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

        CharacterPanelApplicationService service =
                new CharacterPanelApplicationService(authorizeUtil, esiGateway, invTypesService);

        // 断言原样传播,不转换不吞掉
        assertThatThrownBy(() -> service.panel(CHARACTER_ID, false))
                .isInstanceOf(EveHelperException.class)
                .satisfies(ex -> assertThat(((EveHelperException) ex).getResultCode())
                        .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));

        // 网关零调用:越权拒绝发生在取数之前
        verify(esiGateway, never()).getAccessTokenWithExpiry(anyInt(), anyInt());
        verify(esiGateway, never()).queryCharacterPublicInfo(anyInt(), any(Boolean.class));
        verify(esiGateway, never()).queryCharacterOnline(anyInt(), any(), any(Boolean.class));
        verify(esiGateway, never()).queryCharacterLocation(anyInt(), any(), any(Boolean.class));
        verify(esiGateway, never()).queryCharacterShipTypeId(anyInt(), any(), any(Boolean.class));
    }
}
