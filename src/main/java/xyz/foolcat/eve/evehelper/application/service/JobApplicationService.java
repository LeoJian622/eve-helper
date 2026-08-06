package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.service.system.IndustryJobService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

/**
 * 工业制造应用服务
 * 负责制造线数据同步用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    /** type 为 corporation(军团)时按军团同步 */
    private static final String CORP_TYPE = "corporation";

    /** complete 为 complete 时包含已完成任务 */
    private static final String COMPLETE_FLAG = "complete";

    private final IndustryJobService industryJobService;

    private final AccessGuard accessGuard;

    /**
     * 从 ESI 同步制造线数据。
     * <p>
     * 访问控制:id 为用户可控入参,须先确认该人物/军团属于当前用户(防御 IDOR)。
     *
     * @param type     枚举值,人物:char; 公司:corporation
     * @param id       人物或军团 ID
     * @param complete 是否包含已完成任务
     */
    public void syncJobs(String type, Integer id, String complete) {
        accessGuard.requireOwnership(String.valueOf(id), "制造线同步");
        boolean includeCompleted = COMPLETE_FLAG.equals(complete);
        boolean isCorporation = CORP_TYPE.equals(type);
        try {
            industryJobService.batchInsertOrUpdateFromEsi(id, includeCompleted, isCorporation);
        } catch (ParseException e) {
            log.error("制造线同步失败: type={}, id={}", type, id, e);
            throw new EveHelperException("制造线同步失败", e);
        }
    }
}