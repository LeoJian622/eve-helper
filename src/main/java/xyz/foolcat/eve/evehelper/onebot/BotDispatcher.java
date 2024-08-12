package xyz.foolcat.eve.evehelper.onebot;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.onebot.model.MessageEvent;
import xyz.foolcat.eve.evehelper.service.system.InvTypesService;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final InvTypesService invTypesService;

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
                    case ".admAddType":{
                        return addInvTypes(messageEvent, commandMatcher);
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

    private @NotNull JSONObject addInvTypes(MessageEvent messageEvent, Matcher commandMatcher) {
        if (!"359635464".equals(messageEvent.getUser_id().toString())){
            return BotUtil.generateMessage(messageEvent, "没有权限", false);
        }
        InvTypes invTypes = invTypesService.updateTypeByTypeId(Integer.parseInt(commandMatcher.group(2)));
        String message;
        if (invTypes != null) {
            message = "添加物品：" + invTypes.getName() + "成功";
        }else {
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

}
