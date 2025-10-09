package xyz.foolcat.eve.evehelper.infrastructure.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.service.system.StructureService;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.WebSocket;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 建筑相关任务
 *
 * @author Leojan
 * date 2024-06-24 9:20
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class StructTask {

    private final StructureService structureService;

    private final WebSocket webSocket;

    /**
     * 势力铁壁typeId
     */
    private final Set<Long> ignoreTypeIds = new HashSet<>() {{
        add(47513L);
        add(47514L);
        add(47515L);
        add(47516L);
    }};

    /**
     * 更新建築信息
     */
    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void updateStruct() {
        log.info("updateStruct");
        try {
            structureService.batchInsertOrUpdateFromEsi(TaskConstant.CHARACTER_ID);
        } catch (ParseException e) {
            log.error("【建筑】定时更新建筑信息失败：{}", e.getMessage());
        }
    }

    /**
     * 通知24小時燃料耗盡的建築
     */
    @Scheduled(cron = "0 0 18,22 * * ? ")
    public void noticeFuelExpires() {
        log.info("noticeFuelExpires");
        List<Structure> structures = structureService.selectFuelExpiresList(24, 656880659);

        String message = structures.stream().filter(structure -> structure.getFuelExpires() == null)
                .filter(structure -> !ignoreTypeIds.contains(structure.getTypeId()))
                .map(structure -> structure.getName() + ", 燃料耗尽")
                .collect(Collectors.joining("\n"));

        if (StrUtil.isNotEmpty(message)) {
            message += "\n============================\n";
        }

        message = message + structures.stream()
                .filter(structure -> structure.getFuelExpires() != null)
                .sorted(Comparator.comparing(Structure::getFuelExpires))
                .map(structure -> {
                    long between = ChronoUnit.HOURS.between(OffsetDateTime.now(), structure.getFuelExpires());
                    return structure.getName() + ", 将在" + between + "小时后燃料耗尽";
                }).collect(Collectors.joining("\n"));
        if (StrUtil.isEmpty(message)) {
            message = "燃料充足，建筑击毁报告不可用！";
        }
        JSONObject group = BotUtil.generateMessage(null, 41772910L, BotUtil.MESSAGE_TYPE_GROUP, message,false);
        webSocket.sendOneMessage("napcat", group.toJSONString(4));
    }
}
