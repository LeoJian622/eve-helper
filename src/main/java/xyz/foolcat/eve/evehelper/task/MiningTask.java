package xyz.foolcat.eve.evehelper.task;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.eve.Invuniquenames;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.onebot.WebSocket;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.service.eve.InvuniquenamesService;
import xyz.foolcat.eve.evehelper.service.system.StructureService;
import xyz.foolcat.eve.evehelper.vo.ExtractionVO;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 卫星矿堡任务
 *
 * @author Leojan
 * date 2024-06-27 17:22
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class MiningTask {

    private final IndustryApi industryApi;
    private final EsiApiService esiApiService;
    private final StructureService structureService;
    private final WebSocket webSocket;
    private final InvuniquenamesService invuniquenamesService;

    /**
     * 通知卫星矿可开采时间
     */
    @Scheduled(cron = "0 0 21 * * ? ")
    public void noticeExtraction() {
        Integer characterId = 2112818290;
        Integer corporationId = 656880659;
        String accessToken = null;
        try {
            accessToken = esiApiService.getAccessToken(characterId, -1);
        } catch (ParseException e) {
            log.error("【卫星矿通知】AccessToken异常{}", e.getMessage());
        }
        Integer maxPage = industryApi.queryCorporationMiningExtractionsMaxPage(corporationId, EsiClient.SERENITY, accessToken);

        /**
         * 两天后的时间
         */
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime after1Day = startTime.plusDays(1);

        String finalAccessToken = accessToken;
        List<ExtractionVO> extractionVOS = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> industryApi.queryCorporationMiningExtractions(corporationId, EsiClient.SERENITY, i, finalAccessToken).collectList().block())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(chunkTimersResponse -> chunkTimersResponse.getChunkArrivalTime().isBefore(after1Day))
                .map(chunkTimersResponse -> {
                    Structure structure = structureService.getById(chunkTimersResponse.getStructureId());
                    Invuniquenames invuniquenames = invuniquenamesService.getById(chunkTimersResponse.getMoonId());
                    ExtractionVO extractionVO = new ExtractionVO();
                    extractionVO.setChunkArrivalTime(chunkTimersResponse.getChunkArrivalTime());
                    extractionVO.setExtractionStartTime(chunkTimersResponse.getExtractionStartTime());
                    extractionVO.setNaturalDecayTime(chunkTimersResponse.getNaturalDecayTime());
                    extractionVO.setMoonId(chunkTimersResponse.getMoonId());
                    extractionVO.setStructureName(structure.getName());
                    extractionVO.setMoonName(invuniquenames.getItemname());
                    return extractionVO;
                }).collect(Collectors.toList());
        String messages = extractionVOS.stream().map(extractionVO -> extractionVO.getMoonName() + "," + extractionVO.getStructureName() + "将在" + extractionVO.getNaturalDecayTime().atZoneSameInstant(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) + "可以开采")
                .collect(Collectors.joining("\n"));
        if (StrUtil.isNotEmpty(messages)) {
            log.debug("messages: {}", messages);
            JSONObject group = BotUtil.generateMessage(null, 741565463L, BotUtil.MESSAGE_TYPE_GROUP, messages,false);
            webSocket.sendOneMessage("napcat", group.toJSONString(4));
        }
    }
}
