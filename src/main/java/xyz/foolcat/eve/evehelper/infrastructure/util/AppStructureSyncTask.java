package xyz.foolcat.eve.evehelper.infrastructure.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.system.AppStructureSyncService;

/**
 * APP 建筑同步定时任务（T030,US4）。
 * <p>
 * cron 错开 QQ 侧整点(StructTask "0 0 0/1 * * ?")5 分钟,
 * 避免两侧同时拉取 ESI 触发限流。
 *
 * @author Leojan
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppStructureSyncTask {

    private final AppStructureSyncService appStructureSyncService;

    /**
     * 每小时(错开 5 分)触发全军团建筑同步。
     */
    @Scheduled(cron = "0 5 0/1 * * ?")
    public void syncStructures() {
        log.info("【建筑】APP 定时同步开始");
        try {
            appStructureSyncService.syncAllCorps();
        } catch (Exception e) {
            log.error("【建筑】APP 定时同步失败", e);
        }
    }
}
