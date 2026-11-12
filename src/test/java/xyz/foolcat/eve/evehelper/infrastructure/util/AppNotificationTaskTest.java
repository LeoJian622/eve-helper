package xyz.foolcat.eve.evehelper.infrastructure.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.Scheduled;
import xyz.foolcat.eve.evehelper.domain.service.system.AppNotificationDetectService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * APP 通知定时任务单元测试。
 * <p>
 * 验证四个定时方法正确委托给检测服务，并通过反射断言 cron 表达式精确值。
 *
 * @author Leojian
 * date 2026-11-13
 */
class AppNotificationTaskTest {

    @Mock
    private AppNotificationDetectService detectService;

    private AppNotificationTask task;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        task = new AppNotificationTask(detectService);
    }

    @Test
    @DisplayName("燃料检测定时任务委托给 detectFuelLow")
    void task_fuel_delegatesToDetectFuelLow() throws NoSuchMethodException {
        // 调用定时方法
        task.noticeFuelLow();

        // 验证委托
        verify(detectService, times(1)).detectFuelLow();

        // 反射断言 cron 表达式
        Method method = AppNotificationTask.class.getMethod("noticeFuelLow");
        Scheduled[] annotations = method.getAnnotationsByType(Scheduled.class);
        assertThat(annotations).hasSize(1);
        assertThat(annotations[0].cron()).isEqualTo("0 0 18,22 * * ?");
    }

    @Test
    @DisplayName("工业检测定时任务委托给 detectIndustryComplete（双 cron）")
    void task_industry_delegatesToDetectIndustryComplete() throws NoSuchMethodException {
        // 调用定时方法
        task.noticeIndustryComplete();

        // 验证委托
        verify(detectService, times(1)).detectIndustryComplete();

        // 反射断言双 cron 表达式（Spring 6.2 @Repeatable）
        Method method = AppNotificationTask.class.getMethod("noticeIndustryComplete");
        Scheduled[] annotations = method.getAnnotationsByType(Scheduled.class);
        assertThat(annotations).hasSize(2);

        // 断言两个 cron 值均存在（顺序无关）
        assertThat(annotations)
                .extracting(Scheduled::cron)
                .containsExactlyInAnyOrder(
                        "0 0 18,20-23 ? * 1-5",
                        "0 0 8-18,20-23 ? * 0,6"
                );
    }

    @Test
    @DisplayName("技能队列空检测定时任务委托给 detectSkillQueueEmpty")
    void task_skill_delegatesToDetectSkillQueueEmpty() throws NoSuchMethodException {
        // 调用定时方法
        task.noticeSkillQueueEmpty();

        // 验证委托
        verify(detectService, times(1)).detectSkillQueueEmpty();

        // 反射断言 cron 表达式
        Method method = AppNotificationTask.class.getMethod("noticeSkillQueueEmpty");
        Scheduled[] annotations = method.getAnnotationsByType(Scheduled.class);
        assertThat(annotations).hasSize(1);
        assertThat(annotations[0].cron()).isEqualTo("0 10 0/1 * * ?");
    }

    @Test
    @DisplayName("过期清理定时任务委托给 cleanupExpired")
    void task_cleanup_delegatesToCleanupExpired() throws NoSuchMethodException {
        // stub 返回值
        when(detectService.cleanupExpired()).thenReturn(120);

        // 调用定时方法
        task.cleanupExpiredNotifications();

        // 验证委托
        verify(detectService, times(1)).cleanupExpired();

        // 反射断言 cron 表达式
        Method method = AppNotificationTask.class.getMethod("cleanupExpiredNotifications");
        Scheduled[] annotations = method.getAnnotationsByType(Scheduled.class);
        assertThat(annotations).hasSize(1);
        assertThat(annotations[0].cron()).isEqualTo("0 30 3 * * ?");
    }
}
