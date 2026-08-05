package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.IndustryJobService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 工业制造应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工业制造应用服务单元测试")
class JobApplicationServiceUnitTest {

    @Mock
    IndustryJobService industryJobService;

    private JobApplicationService jobApplicationService;

    @BeforeEach
    void setUp() {
        jobApplicationService = new JobApplicationService(industryJobService);
    }

    @Test
    @DisplayName("同步人物任务 -> 按 isCor=false / includeCompleted=false 调用")
    void syncJobs_character() throws Exception {
        jobApplicationService.syncJobs("char", 5, "running");

        verify(industryJobService).batchInsertOrUpdateFromEsi(5, false, false);
    }

    @Test
    @DisplayName("同步军团已完成任务 -> 按 isCor=true / includeCompleted=true 调用")
    void syncJobs_corporationComplete() throws Exception {
        jobApplicationService.syncJobs("corporation", 5, "complete");

        verify(industryJobService).batchInsertOrUpdateFromEsi(5, true, true);
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncJobs_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(industryJobService).batchInsertOrUpdateFromEsi(5, false, false);

        assertThrows(EveHelperException.class,
                () -> jobApplicationService.syncJobs("char", 5, "running"));
    }
}