package xyz.foolcat.eve.evehelper.infrastructure.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import xyz.foolcat.eve.evehelper.domain.service.system.AppStructureSyncService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppStructureSyncTask 单元测试（T030,US4 建筑同步节流任务）。
 * <p>
 * 验证任务委托给服务,且 cron 表达式错开 QQ 侧整点 5 分钟。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
class AppStructureSyncTaskTest {

    @Mock
    private AppStructureSyncService appStructureSyncService;

    @Test
    void task_delegatesToService() throws NoSuchMethodException {
        // Arrange
        AppStructureSyncTask task = new AppStructureSyncTask(appStructureSyncService);

        // Act
        task.syncStructures();

        // Assert:委托给 service
        verify(appStructureSyncService, times(1)).syncAllCorps();

        // cron 反射断言:错开 QQ 侧整点 5 分钟
        String cron = AppStructureSyncTask.class
                .getMethod("syncStructures")
                .getAnnotation(Scheduled.class)
                .cron();
        assertThat(cron).isEqualTo("0 5 0/1 * * ?");
    }
}
