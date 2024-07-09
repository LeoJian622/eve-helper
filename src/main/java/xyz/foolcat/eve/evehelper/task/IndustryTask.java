package xyz.foolcat.eve.evehelper.task;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.system.IndustryJob;
import xyz.foolcat.eve.evehelper.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.onebot.WebSocket;
import xyz.foolcat.eve.evehelper.service.system.IndustryJobService;

import java.text.ParseException;
import java.time.OffsetDateTime;
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
            industryJobService.batchInsertOrUpdateFromEsi(98061457, true, true);

        } catch (ParseException e) {
            log.error("【工业】定时更新工业信息失败：{}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0/20 18-23 ? * 1-5 ")
    @Scheduled(cron = "0 0/20 8-23 ? * 0,6 ")
    public void noticeJobComplete() {
        List<IndustryJob> industryJobs = industryJobService.lambdaQuery().eq(IndustryJob::getCorporationId, 98061457)
                .ne(IndustryJob::getStatus, IndustryJob.STATUS_DELIVERED)
                .list();
        String message = industryJobs.stream()
                .filter(industryJob -> industryJob.getStatus().equals(IndustryJob.STATUS_ACTIVE) && industryJob.getEndDate().isBefore(OffsetDateTime.now().plusHours(24)))
                .map(industryJob -> industryJob.getProductType() + " * " + industryJob.getRuns() + ",完成时间预计为：" + industryJob.getEndDate().atZoneSameInstant(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                .collect(Collectors.joining("\n"));
        message = "已完成或预计24小时内完成：\n" + message;
        JSONObject group = BotUtil.generateMessage(null, 41772910L, BotUtil.MESSAGE_TYPE_GROUP, message,false);
        webSocket.sendOneMessage("napcat", group.toJSONString(4));
    }

}
