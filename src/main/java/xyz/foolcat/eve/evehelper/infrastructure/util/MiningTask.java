package xyz.foolcat.eve.evehelper.infrastructure.util;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.domain.service.eve.InvuniquenamesService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.StructureService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.WebSocket;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.ExtractionVO;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
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

    private final EveAccountService eveAccountService;

    private final AuthorizeUtil authorizeUtil;

    /**
     * 通知卫星矿可开采时间
     */
    @Scheduled(cron = "0 0 19 * * ? ")
    public void noticeExtraction() {
        log.info("noticeExtraction");
        EveAccount eveAccount = authorizeUtil.authorize(TaskConstant.CHARACTER_ID);
        String accessToken = null;
        try {
            accessToken = esiApiService.getAccessToken(TaskConstant.CHARACTER_ID, eveAccount.getUserId());
        } catch (ParseException e) {
            log.error("【卫星矿通知】AccessToken异常{}", e.getMessage());
        }
        String messages = requestMiningExtractable(eveAccount.getCorpId(), 1,accessToken);
        if (StrUtil.isNotEmpty(messages)) {
            log.debug("messages: {}", messages);
            JSONObject group = BotUtil.generateMessage(null, 741565463L, BotUtil.MESSAGE_TYPE_GROUP, messages,false);
            webSocket.sendOneMessage("napcat", group.toJSONString(4));
        }
    }


    /**
     * 通知卫星矿可开采时间
     */
    @Scheduled(cron = "0 0 19 * * 1 ")
    public void noticeExtraction7Day() {
        log.info("noticeExtraction");
        Integer characterId = 2112818290;
        EveAccount eveAccount = authorizeUtil.authorize(characterId);
        String accessToken = null;
        try {
            accessToken = esiApiService.getAccessToken(characterId, eveAccount.getUserId());
        } catch (ParseException e) {
            log.error("【卫星矿通知】AccessToken异常{}", e.getMessage());
        }
        String messages = requestMiningExtractable(eveAccount.getCorpId(), 7,accessToken);
        if (StrUtil.isNotEmpty(messages)) {
            log.debug("messages: {}", messages);
            JSONObject group = BotUtil.generateMessage(null, 741565463L, BotUtil.MESSAGE_TYPE_GROUP, messages,false);
            webSocket.sendOneMessage("napcat", group.toJSONString(4));
        }
    }





    private @NotNull String requestMiningExtractable(Integer corporationId, Integer day, String accessToken) {
        Integer maxPage = industryApi.queryCorporationMiningExtractionsMaxPage(corporationId, EsiClient.SERENITY, accessToken);

        /*
         * day天后的时间
         */
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime after1Day = startTime.plusDays(day);

        List<ExtractionVO> extractionVOS = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> industryApi.queryCorporationMiningExtractions(corporationId, EsiClient.SERENITY, i, accessToken).collectList().block())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(chunkTimersResponse -> chunkTimersResponse.getChunkArrivalTime().isBefore(after1Day))
                .map(chunkTimersResponse -> {
                    Structure structure = structureService.selectById(chunkTimersResponse.getStructureId());
                    InvUniqueNames invuniquenames = invuniquenamesService.selectById(chunkTimersResponse.getMoonId());
                    ExtractionVO extractionVO = new ExtractionVO();
                    extractionVO.setChunkArrivalTime(chunkTimersResponse.getChunkArrivalTime());
                    extractionVO.setExtractionStartTime(chunkTimersResponse.getExtractionStartTime());
                    extractionVO.setNaturalDecayTime(chunkTimersResponse.getNaturalDecayTime());
                    extractionVO.setMoonId(chunkTimersResponse.getMoonId());
                    extractionVO.setStructureName(structure.getName());
                    extractionVO.setMoonName(invuniquenames.getItemName());
                    return extractionVO;
                }).sorted(Comparator.comparing(ExtractionVO::getNaturalDecayTime))
                .collect(Collectors.toList());
        return extractionVOS.stream().map(extractionVO -> extractionVO.getMoonName() + "," + extractionVO.getStructureName() + "将在" + extractionVO.getNaturalDecayTime().atZoneSameInstant(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) + "可以开采")
                .collect(Collectors.joining("\n"));
    }
}
