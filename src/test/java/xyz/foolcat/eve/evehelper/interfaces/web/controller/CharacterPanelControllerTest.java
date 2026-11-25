package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.foolcat.eve.evehelper.application.service.CharacterPanelApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAttributesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPanelInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillList;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.interfaces.web.advice.GlobalExceptionHandler;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US1 角色面板控制器失败测试(RED-only, T008)。
 * <p>
 * 五个用例固化三端点行为:
 * GET /character/{characterId}/panel | skills | skill-queue。
 * <p>
 * RED 原因:CharacterPanelController 与三个响应 DTO
 * (CharacterPanelResponse / CharacterSkillsResponse / SkillQueueResponse)
 * 不存在,编译失败即 RED 证据。生产代码由 T013 创建。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US1 角色面板控制器测试")
class CharacterPanelControllerTest {

    @Mock
    private CharacterPanelApplicationService panelService;

    @Mock
    private InvTypesService invTypesService;

    private MockMvc mockMvc;

    private static final Integer CHARACTER_ID = 2112818290;
    private static final OffsetDateTime BIRTHDAY = OffsetDateTime.parse("2020-01-15T10:30:00Z");
    private static final OffsetDateTime LAST_LOGIN = OffsetDateTime.parse("2026-10-19T08:00:00Z");
    private static final OffsetDateTime LAST_LOGOUT = OffsetDateTime.parse("2026-10-18T22:00:00Z");
    private static final OffsetDateTime QUEUE_END = OffsetDateTime.parse("2026-10-25T12:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CharacterPanelController(panelService, invTypesService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------
    //  panel
    // ---------------------------------------------------------------

    @Test
    @DisplayName("panel 返回 Result 信封, data 含嵌套 location/ship")
    void panel_returnsResultEnvelope() throws Exception {
        // Arrange
        CharacterPanelInfo panelInfo = new CharacterPanelInfo(
                CHARACTER_ID, "Test Pilot", 98000001, 99000001,
                BIRTHDAY, 2.5, true, LAST_LOGIN, LAST_LOGOUT,
                30000142, 60003760, null, 670, "Rifter"
        );
        when(panelService.panel(CHARACTER_ID, false)).thenReturn(panelInfo);

        // Act & Assert — contracts §1 panel 响应形状
        mockMvc.perform(get("/character/{characterId}/panel", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.characterId").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.name").value("Test Pilot"))
                .andExpect(jsonPath("$.data.corporationId").value(98000001))
                .andExpect(jsonPath("$.data.allianceId").value(99000001))
                .andExpect(jsonPath("$.data.birthday").exists())
                .andExpect(jsonPath("$.data.securityStatus").value(2.5))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.lastLogin").exists())
                .andExpect(jsonPath("$.data.lastLogout").exists())
                // nested location
                .andExpect(jsonPath("$.data.location.solarSystemId").value(30000142))
                .andExpect(jsonPath("$.data.location.stationId").value(60003760))
                .andExpect(jsonPath("$.data.location.structureId").doesNotExist())
                // nested ship (typeId 非 shipTypeId)
                .andExpect(jsonPath("$.data.ship.typeId").value(670))
                .andExpect(jsonPath("$.data.ship.name").value("Rifter"));

        verify(panelService).panel(CHARACTER_ID, false);
    }

    // ---------------------------------------------------------------
    //  skills
    // ---------------------------------------------------------------

    @Test
    @DisplayName("skills 返回属性与技能列表")
    void skills_returnsAttributesAndSkillList() throws Exception {
        // Arrange
        CharacterAttributesInfo attributes = new CharacterAttributesInfo(
                25, 24, 20, 22, 23, 2
        );
        CharacterSkillList.SkillItem skill = new CharacterSkillList.SkillItem(
                3300, 3, 3, 256000L
        );
        CharacterSkillsInfo skillsInfo = new CharacterSkillsInfo(
                50000000L, 500000, attributes, List.of(skill)
        );
        when(panelService.skills(CHARACTER_ID, false)).thenReturn(skillsInfo);

        // Act & Assert — contracts §1 skills 响应形状
        mockMvc.perform(get("/character/{characterId}/skills", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalSp").value(50000000))
                .andExpect(jsonPath("$.data.unallocatedSp").value(500000))
                // attributes
                .andExpect(jsonPath("$.data.attributes.intelligence").value(25))
                .andExpect(jsonPath("$.data.attributes.memory").value(24))
                .andExpect(jsonPath("$.data.attributes.charisma").value(20))
                .andExpect(jsonPath("$.data.attributes.perception").value(22))
                .andExpect(jsonPath("$.data.attributes.willpower").value(23))
                .andExpect(jsonPath("$.data.attributes.bonusRemaps").value(2))
                // skills array
                .andExpect(jsonPath("$.data.skills").isArray())
                .andExpect(jsonPath("$.data.skills[0].skillId").value(3300))
                .andExpect(jsonPath("$.data.skills[0].trainedSkillLevel").value(3))
                .andExpect(jsonPath("$.data.skills[0].activeSkillLevel").value(3))
                .andExpect(jsonPath("$.data.skills[0].skillpointsInSkill").value(256000));

        verify(panelService).skills(CHARACTER_ID, false);
    }

    // ---------------------------------------------------------------
    //  skill-queue
    // ---------------------------------------------------------------

    @Test
    @DisplayName("空技能队列返回 queue:[] 与 null queueEndsAt")
    void skillQueue_empty_returnsEmptyArray() throws Exception {
        // Arrange — 空队列:queue 为空列表, queueEndsAt 为 null
        SkillQueueInfo emptyQueue = new SkillQueueInfo(Collections.emptyList(), null);
        when(panelService.skillQueue(CHARACTER_ID, false)).thenReturn(emptyQueue);

        // Act & Assert — contracts §1 skill-queue 响应形状(空队列)
        mockMvc.perform(get("/character/{characterId}/skill-queue", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.queue").isArray())
                .andExpect(jsonPath("$.data.queue").isEmpty())
                .andExpect(jsonPath("$.data.queueEndsAt").doesNotExist());

        verify(panelService).skillQueue(CHARACTER_ID, false);
    }

    // ---------------------------------------------------------------
    //  forceRefresh 透传
    // ---------------------------------------------------------------

    @Test
    @DisplayName("forceRefresh=true 透传至服务层;缺省为 false")
    void forceRefresh_passedToService() throws Exception {
        // Arrange
        SkillQueueInfo queueInfo = new SkillQueueInfo(
                List.of(new SkillQueueItem(
                        3301, 4, 256000L, 1024000L,
                        OffsetDateTime.now(), QUEUE_END, 0
                )),
                QUEUE_END
        );
        when(panelService.skillQueue(CHARACTER_ID, true)).thenReturn(queueInfo);
        when(panelService.skillQueue(CHARACTER_ID, false)).thenReturn(queueInfo);

        // Act 1 — ?forceRefresh=true → verify 服务收到 true
        mockMvc.perform(get("/character/{characterId}/skill-queue", CHARACTER_ID)
                        .param("forceRefresh", "true"))
                .andExpect(status().isOk());
        verify(panelService).skillQueue(CHARACTER_ID, true);

        // Act 2 — 缺省 → verify 服务收到 false
        mockMvc.perform(get("/character/{characterId}/skill-queue", CHARACTER_ID))
                .andExpect(status().isOk());
        verify(panelService).skillQueue(CHARACTER_ID, false);
    }

    // ---------------------------------------------------------------
    //  归属违规 → 403
    // ---------------------------------------------------------------

    @Test
    @DisplayName("归属校验失败(ACCESS_UNAUTHORIZED)返回 HTTP 403")
    void ownershipViolation_returns403() throws Exception {
        // Arrange — 服务层抛出 EveHelperException(ACCESS_UNAUTHORIZED)
        when(panelService.panel(CHARACTER_ID, false))
                .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

        // Act & Assert — GlobalExceptionHandler 映射 ACCESS_UNAUTHORIZED → 403
        mockMvc.perform(get("/character/{characterId}/panel", CHARACTER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCode.ACCESS_UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
