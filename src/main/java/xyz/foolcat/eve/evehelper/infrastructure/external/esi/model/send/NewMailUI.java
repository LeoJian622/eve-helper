package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 打开新建邮件窗口
 *
 * @author Leojan
 * date 2023-11-19 20:20
 */

@Data
@Tag(name = "打开新建邮件窗口")
public class NewMailUI {

    private String body;

    private List<Integer> recipients;

    private String subject;


    @JsonProperty("to_corp_or_alliance_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer toCorpOrAllianceId;

    @JsonProperty("to_mailing_list_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer toMailingListId;
}
