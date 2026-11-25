package xyz.foolcat.eve.evehelper.infrastructure.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.system.AppNotificationDetectService;

/**
 * APP 通知定时任务。
 * <p>
 * 薄委托层：调用 {@link AppNotificationDetectService} 四方法，外层 catch 兜底防单任务崩溃。
 * cron 表达式精确匹配 brief 任务原文（无尾随空格）。
 *
 * @author Leojian
 * date 2026-11-13
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppNotificationTask {

    private final AppNotificationDetectService detectService;

    /**
     * 燃料不足检测（每日 18:00 / 22:00）。
     */
    @Scheduled(cron = "0 0 18,22 * * ?")
    public void noticeFuelLow() {
        log.info("【通知】燃料检测定时触发");
        try {
            detectService.detectFuelLow();
        } catch (Exception e) {
            log.error("【通知】燃料检测失败", e);
        }
    }

    /**
     * 工业完成检测（工作日 18:00/20-23 点，周末 8-18/20-23 点）。
     */
    @Scheduled(cron = "0 0 18,20-23 ? * 1-5")
    @Scheduled(cron = "0 0 8-18,20-23 ? * 0,6")
    public void noticeIndustryComplete() {
        log.info("【通知】工业检测定时触发");
        try {
            detectService.detectIndustryComplete();
        } catch (Exception e) {
            log.error("【通知】工业检测失败", e);
        }
    }

    /**
     * 技能队列空检测（每小时第 10 分钟）。
     */
    @Scheduled(cron = "0 10 0/1 * * ?")
    public void noticeSkillQueueEmpty() {
        log.info("【通知】技能队列检测定时触发");
        try {
            detectService.detectSkillQueueEmpty();
        } catch (Exception e) {
            log.error("【通知】技能队列检测失败", e);
        }
    }

    /**
     * 过期通知清理（每日凌晨 3:30）。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpiredNotifications() {
        log.info("【通知】过期清理定时触发");
        try {
            detectService.cleanupExpired();
        } catch (Exception e) {
            log.error("【通知】过期清理失败", e);
        }
    }
}
