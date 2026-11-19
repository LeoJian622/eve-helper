package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.vo.TaxReturnResult;
import xyz.foolcat.eve.evehelper.domain.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.domain.service.system.StructureService;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QQBotMessageDispatcher {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("(^\\.[a-zA-Z]+)\\s+(.*)");

    private final InvTypesService invTypesService;
    private final WalletJournalService walletJournalService;
    private final StructureService structureService;
    private final AuthorizeUtil authorizeUtil;
    private final QQBotIntegrationProperties properties;

    public Optional<String> dispatch(QQBotMessageEvent event) {
        if (event == null || event.content() == null) {
            return Optional.empty();
        }

        Matcher commandMatcher = COMMAND_PATTERN.matcher(event.content());
        if (commandMatcher.matches()) {
            return Optional.ofNullable(switch (commandMatcher.group(1)) {
                case ".jita" -> queryJitaPrice(commandMatcher);
                case ".admAddType" -> addInvTypes(event, commandMatcher);
                case ".tax" -> queryTax(commandMatcher);
                case ".struct" -> queryStructure(event);
                default -> null;
            });
        }
        if (event.content().startsWith("TESTBOT")) {
            return Optional.of(event.content().substring("TESTBOT".length()));
        }
        return Optional.empty();
    }

    private String queryStructure(QQBotMessageEvent event) {
        if (!properties.isAdmin(event.authorOpenid())) {
            return "垫下：母鸡呀";
        }
        EveAccount eveAccount = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, 2112818290);
        List<Structure> structures = structureService.selectFuelExpiresList(24, eveAccount.getCorpId());

        String message = structures.stream()
                .filter(structure -> structure.getFuelExpires() == null)
                .map(structure -> structure.getName() + ", 燃料耗尽")
                .collect(Collectors.joining("\n"));
        message += "\n============================\n";
        message += structures.stream()
                .filter(structure -> structure.getFuelExpires() != null)
                .sorted(Comparator.comparing(Structure::getFuelExpires))
                .map(structure -> {
                    long between = ChronoUnit.HOURS.between(OffsetDateTime.now(), structure.getFuelExpires());
                    return structure.getName() + ", 将在" + between + "小时后燃料耗尽";
                })
                .collect(Collectors.joining("\n"));

        return StrUtil.isEmpty(message) ? "燃料充足，建筑击毁报告不可用！" : message;
    }

    private String addInvTypes(QQBotMessageEvent event, Matcher commandMatcher) {
        if (!properties.isAdmin(event.authorOpenid())) {
            return "没有权限";
        }
        InvTypes invTypes = invTypesService.updateTypeByTypeId(Integer.parseInt(commandMatcher.group(2)));
        if (invTypes != null) {
            return "添加物品：" + invTypes.getName() + "成功";
        }
        return "添加ID：" + commandMatcher.group(2) + "失败";
    }

    private String queryJitaPrice(Matcher commandMatcher) {
        InvTypes invTypes = invTypesService.selectOneByName(commandMatcher.group(2));
        WebClient request = WebClient.builder().build();
        JSONObject price = request.get()
                .uri("https://www.ceve-market.org/api/market/region/{reg}/type/{id}.json", 10000002, invTypes.getTypeId())
                .retrieve()
                .bodyToMono(JSONObject.class)
                .block();
        if (price == null) {
            return "未查询到市场价格";
        }
        DecimalFormat decimalFormat = new DecimalFormat("#,###.00");
        BigDecimal buyMax = BigDecimal.valueOf(price.getJSONObject("buy").getDouble("max"));
        BigDecimal sellMin = BigDecimal.valueOf(price.getJSONObject("sell").getDouble("min"));
        return "物品国服售价（伏尔戈）：\n物品名称：" + invTypes.getName()
                + "\n收单价：" + decimalFormat.format(buyMax)
                + "\n卖单价：" + decimalFormat.format(sellMin)
                + "\n中位价：" + decimalFormat.format(
                        buyMax.add(sellMin).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP));
    }

    private String queryTax(Matcher commandMatcher) {
        String[] arg = commandMatcher.group(2).split(" ");
        if (arg.length != 3) {
            return "参数为：军团税 现有税 年月(202411)";
        }
        try {
            List<TaxReturnResult> taxReturnResults = walletJournalService.countBoundsReturn(arg[0], arg[1], arg[2]);
            StringBuilder message = new StringBuilder("人物\t退税\n");
            for (TaxReturnResult tax : taxReturnResults) {
                message.append(tax.name()).append("\t").append(tax.amount()).append("\n");
            }
            return message.toString();
        } catch (ParseException e) {
            log.error("QQ Bot tax command failed", e);
            return e.getMessage();
        }
    }
}
