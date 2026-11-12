package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.foolcat.eve.evehelper.application.service.MemberTrackingApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingInfo;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 团员追踪控制器单元测试(T038, 2 用例)。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US5 团员追踪控制器测试")
class MemberTrackingControllerTest {

    @Mock
    private MemberTrackingApplicationService memberTrackingApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MemberTrackingController(memberTrackingApplicationService))
                .build();
    }

    // ---------------------------------------------------------------
    //  GET /corporation/{corpId}/member-tracking — 正常返回信封
    // ---------------------------------------------------------------

    @Test
    @DisplayName("tracking 返回 Result 信封含 members 与 trackedAt")
    void tracking_returnsResultEnvelope() throws Exception {
        OffsetDateTime trackedAt = OffsetDateTime.parse("2026-11-13T10:00:00Z");
        MemberTrackingInfo info = new MemberTrackingInfo(
                List.of(new MemberTrackingInfo.MemberTrackingMember(
                        2001, true, 60001L, 670,
                        OffsetDateTime.parse("2026-11-13T09:00:00Z"),
                        OffsetDateTime.parse("2026-11-13T08:00:00Z")
                )),
                trackedAt
        );

        when(memberTrackingApplicationService.track(500, 1001, false)).thenReturn(info);

        mockMvc.perform(get("/corporation/500/member-tracking?characterId=1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.members[0].characterId").value(2001))
                .andExpect(jsonPath("$.data.members[0].online").value(true))
                .andExpect(jsonPath("$.data.trackedAt").exists());

        verify(memberTrackingApplicationService).track(500, 1001, false);
    }

    // ---------------------------------------------------------------
    //  GET /corporation/{corpId}/member-tracking — 缺 characterId → 400
    // ---------------------------------------------------------------

    @Test
    @DisplayName("tracking 缺 characterId 返回 400")
    void tracking_missingCharacterId_400() throws Exception {
        mockMvc.perform(get("/corporation/500/member-tracking"))
                .andExpect(status().isBadRequest());

        verify(memberTrackingApplicationService, never()).track(500, null, false);
    }
}
