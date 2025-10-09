package xyz.foolcat.eve.evehelper.infrastructure.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.service.system.IndustryJobService;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.WebSocket;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工业相关
 *
 * @author Leojan
 * date 2024-07-03 11:28
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class IndustryTask {

    private final IndustryJobService industryJobService;

    private final WebSocket webSocket;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateIndustryJobs() {
        log.info("update industry jobs");
        try {
            industryJobService.batchInsertOrUpdateFromEsi(TaskConstant.CHARACTER_ID, true, true);

        } catch (ParseException e) {
            log.error("【工业】定时更新工业信息失败：{}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 18,20-23 ? * 1-5 ")
    @Scheduled(cron = "0 0 8-18,20-23 ? * 0,6 ")
    public void noticeJobComplete0() {
        List<IndustryJob> industryJobs = industryJobService.selectByCorpIdAndStatus(98061457, IndustryJob.STATUS_DELIVERED);
        JSONObject group = queryCompeleteAfterHour(industryJobs, 0);
        if (!group.isEmpty()) {
            webSocket.sendOneMessage("napcat", group.toJSONString(4));
        }
    }

    @Scheduled(cron = "0 0 19 * * ? ")
    public void noticeJobComplete24() {
        List<IndustryJob> industryJobs = industryJobService.selectByCorpIdAndStatus(98061457, IndustryJob.STATUS_DELIVERED);
        JSONObject group = queryCompeleteAfterHour(industryJobs, 24);
        if (!group.isEmpty()) {
            webSocket.sendOneMessage("napcat", group.toJSONString(4));
        }
    }

    private static @NotNull JSONObject queryCompeleteAfterHour(List<IndustryJob> industryJobs, Integer hour) {
        String message = industryJobs.stream()
                .filter(industryJob ->
                        industryJob.getStatus().equals(IndustryJob.STATUS_ACTIVE) && industryJob.getEndDate().isBefore(OffsetDateTime.now(ZoneId.of("+0")).plusHours(hour))
                )
                .map(industryJob -> industryJob.getProductType() + " * " + industryJob.getRuns() + ",完成时间预计为：" + industryJob.getEndDate().atZoneSameInstant(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                .collect(Collectors.joining("\n"));
        if (StrUtil.isEmpty(message)){
            return new JSONObject();
        }
        message = "已完成或预计24小时内完成：\n" + message;
        return BotUtil.generateMessage(null, 41772910L, BotUtil.MESSAGE_TYPE_GROUP, message,false);
    }

}
