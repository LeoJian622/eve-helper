package xyz.foolcat.eve.evehelper.task;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.onebot.WebSocket;
import xyz.foolcat.eve.evehelper.service.system.StructureService;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 建筑相关任务
 *
 * @author Leojan
 * date 2024-06-24 9:20
 */

@Component
@RequiredArgsConstructor
public class StructTask {

    private final StructureService structureService;

    private final WebSocket webSocket;

    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void updateStruct() throws ParseException {
        structureService.esiBatchInsert(2112818290);
    }

    @Scheduled(cron = "0 0 18,22 * * ? ")
    public void noticeFuelExpires() {

        List<Structure> structures = structureService.selectFuelExpiresList(24, 656880659);
        String message = structures.stream().filter(structure -> structure.getFuelExpires() == null)
                .map(structure -> structure.getName() + ", 燃料耗尽")
                .collect(Collectors.joining("\n"));

        message = message + "\n============================\n" + structures.stream()
                .filter(structure -> structure.getFuelExpires() != null)
                .sorted(Comparator.comparing(Structure::getFuelExpires))
                .map(structure -> {
                    long between = ChronoUnit.HOURS.between(OffsetDateTime.now(), structure.getFuelExpires());
                    return structure.getName() + ", 将在" + between + "小时后燃料耗尽";
                }).collect(Collectors.joining("\n"));
        JSONObject group = BotUtil.generateMessage(null, 41772910L, BotUtil.MESSAGE_TYPE_GROUP, message);

        webSocket.sendOneMessage("napcat", group.toJSONString(4));
    }
}
