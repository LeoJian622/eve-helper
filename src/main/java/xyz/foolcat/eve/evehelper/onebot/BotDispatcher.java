package xyz.foolcat.eve.evehelper.onebot;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.dto.system.TaxReturnDTO;
import xyz.foolcat.eve.evehelper.onebot.model.MessageEvent;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.service.system.StructureService;
import xyz.foolcat.eve.evehelper.service.system.WalletJournalService;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 消息分发器
 *
 * @author Leojan
 * date 2024-06-21 16:38
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class BotDispatcher {

    private final Pattern cqPattern = Pattern.compile("^\\[CQ:.*");
    private final Pattern commandPattern = Pattern.compile("(^\\.[a-zA-Z]+)\\s(.*)");

    private final EveAccountService eveAccountService;

    private final InvTypesService invTypesService;

    private final WalletJournalService walletJournalService;

    private final StructureService structureService;

    public JSONObject dispatchers(MessageEvent messageEvent) {
        if ("meta_event".equals(messageEvent.getPost_type()) || messageEvent.getRaw_message() == null) {
            return null;
        }
        Matcher matcher = cqPattern.matcher(messageEvent.getRaw_message());
        if (matcher.matches()) {
            log.debug("[CQ]代码");
            return null;
        } else {
            Matcher commandMatcher = commandPattern.matcher(messageEvent.getRaw_message());
            if (commandMatcher.matches()) {
                switch (commandMatcher.group(1)) {
                    case ".jita": {
                        return queryJitaPrice(messageEvent, commandMatcher);
                    }
                    case ".admAddType": {
                        return addInvTypes(messageEvent, commandMatcher);
                    }
                    case ".tax": {
                        return queryTax(messageEvent, commandMatcher);
                    }
                    case ".struct": {
                        return queryStructure(messageEvent, commandMatcher);
                    }
                    default:
                        break;
                }
            } else if (messageEvent.getRaw_message().startsWith("TESTBOT")) {
                log.debug("text{}", messageEvent.getRaw_message());
                return BotUtil.generateMessage(messageEvent, messageEvent.getRaw_message().substring(7), false);
            }
        }
        return null;
    }

    private JSONObject queryStructure(MessageEvent messageEvent, Matcher commandMatcher) {

        EveAccount eveAccount = eveAccountService.getOne(new QueryWrapper<EveAccount>().lambda().eq(EveAccount::getCharacterName, commandMatcher.group(2)));

        List<Structure> structures = structureService.selectFuelExpiresList(24, eveAccount.getCorpId());

        String message = structures.stream().filter(structure -> structure.getFuelExpires() == null)
                .map(structure -> structure.getName() + ", 燃料耗尽")
                .collect(Collectors.joining("\n"));

            message += "\n============================\n";

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
        return BotUtil.generateMessage(messageEvent, message,false);
    }

    private @NotNull JSONObject addInvTypes(MessageEvent messageEvent, Matcher commandMatcher) {
        if (!"359635464".equals(messageEvent.getUser_id().toString())) {
            return BotUtil.generateMessage(messageEvent, "没有权限", false);
        }
        InvTypes invTypes = invTypesService.updateTypeByTypeId(Integer.parseInt(commandMatcher.group(2)));
        String message;
        if (invTypes != null) {
            message = "添加物品：" + invTypes.getName() + "成功";
        } else {
            message = "添加ID：" + commandMatcher.group(2) + "失败";
        }
        return BotUtil.generateMessage(messageEvent, message, false);
    }

    private @NotNull JSONObject queryJitaPrice(MessageEvent messageEvent, Matcher commandMatcher) {
        InvTypes invTypes = invTypesService.getOne(new QueryWrapper<InvTypes>().lambda().eq(InvTypes::getName, commandMatcher.group(2)));
        WebClient request = WebClient.builder().build();
        JSONObject price = request.get().uri("https://www.ceve-market.org/api/market/region/{reg}/type/{id}.json", 10000002, invTypes.getTypeId())
                .retrieve().bodyToMono(JSONObject.class).block();
        DecimalFormat decimalFormat = new DecimalFormat("#,###.00");
        assert price != null;
        BigDecimal buyMax = BigDecimal.valueOf(price.getJSONObject("buy").getDouble("max"));
        BigDecimal sellMin = BigDecimal.valueOf(price.getJSONObject("sell").getDouble("min"));
        String message = "物品国服售价（伏尔戈）：\n物品名称：" + invTypes.getName() + "\n收单价：" + decimalFormat.format(buyMax) + "\n卖单价：" + decimalFormat.format(sellMin) + "\n中位价：" + decimalFormat.format(buyMax.add(sellMin).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP));
        return BotUtil.generateMessage(messageEvent, message, false);
    }

    private @NotNull JSONObject queryTax(MessageEvent messageEvent, Matcher commandMatcher) {
        String[] arg = commandMatcher.group(2).split(" ");
        if (arg.length != 3) {
            return BotUtil.generateMessage(messageEvent, "参数为：军团税 现有税 年月(202411)", false);
        }
        try {
            List<TaxReturnDTO> taxReturnDTOS = walletJournalService.countBoundsReturn(arg[0], arg[1], arg[2]);
            StringBuilder message = new StringBuilder("人物\t退税\n");
            for (TaxReturnDTO tax :
                    taxReturnDTOS) {
                message.append(tax.getName()).append("\t").append(tax.getAmount()).append("\n");
            }
            return BotUtil.generateMessage(messageEvent, message.toString(), false);
        } catch (ParseException e) {
            log.error(e.getMessage());
            return BotUtil.generateMessage(messageEvent, e.getMessage(), false);
        }
    }

}
