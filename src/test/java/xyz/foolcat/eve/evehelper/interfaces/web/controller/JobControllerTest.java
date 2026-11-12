package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse.IndustryJobItem;
import xyz.foolcat.eve.evehelper.application.service.IndustryJobQueryApplicationService;
import xyz.foolcat.eve.evehelper.application.service.JobApplicationService;
import xyz.foolcat.eve.evehelper.interfaces.web.advice.GlobalExceptionHandler;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JobController 测试(022 T022)。
 *
 * <p>两个用例:
 * <ol>
 *   <li>getJobs_returnsResultEnvelope — GET /job/{type}/{id} 返回信封+jobs/total 形状</li>
 *   <li>postSync_unchanged — POST /job/{type}/{id}/{complete} 既有链路零改动</li>
 * </ol>
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobController 工业直查+同步(022 US3)")
class JobControllerTest {

    @Mock
    private JobApplicationService jobApplicationService;

    @Mock
    private IndustryJobQueryApplicationService industryJobQueryService;

    private MockMvc mockMvc;

    private static final Integer CHARACTER_ID = 95465499;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new JobController(jobApplicationService, industryJobQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("getJobs 返回 Result 信封,data 含 jobs/total")
    void getJobs_returnsResultEnvelope() throws Exception {
        // Arrange
        IndustryJobItem item = new IndustryJobItem(
                1L, 1, "active", 587, 10,
                OffsetDateTime.parse("2026-11-10T08:00:00Z"),
                OffsetDateTime.parse("2026-11-11T08:00:00Z"),
                (long) CHARACTER_ID, 60003760L
        );
        IndustryJobPageResponse pageResponse = new IndustryJobPageResponse(List.of(item), 1);
        when(industryJobQueryService.queryJobs(
                eq("char"), eq(CHARACTER_ID), isNull(),
                eq(PageQuery.builder().current(1).size(20).build()),
                eq(false)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/job/{type}/{id}", "char", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.jobs").isArray())
                .andExpect(jsonPath("$.data.jobs[0].jobId").value(1))
                .andExpect(jsonPath("$.data.jobs[0].activityId").value(1))
                .andExpect(jsonPath("$.data.jobs[0].status").value("active"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(industryJobQueryService).queryJobs(
                eq("char"), eq(CHARACTER_ID), isNull(),
                eq(PageQuery.builder().current(1).size(20).build()),
                eq(false));
    }

    @Test
    @DisplayName("postSync 既有链路零改动:POST /job/{type}/{id}/{complete}")
    void postSync_unchanged() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/job/{type}/{id}/{complete}", "char", CHARACTER_ID, "complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(jobApplicationService).syncJobs("char", CHARACTER_ID, "complete", false);
    }
}
