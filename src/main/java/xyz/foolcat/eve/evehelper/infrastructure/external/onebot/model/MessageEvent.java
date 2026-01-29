package xyz.foolcat.eve.evehelper.infrastructure.external.onebot.model;

import cn.hutool.json.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 消息事件
 *
 * @author Leojan
 * @date 2024-06-21 15:57
 */

@Data
@Tag(name = "onebot 消息事件")
public class MessageEvent {

    private Long time;

    private Long self_id;

    private String post_type;

    private String message_type;

    private String meta_event_type;

    private String sub_type;

    private Integer message_id;

    private Long user_id;

    private Long group_id;

    private List<JSONObject> message;

    private String raw_message;

    private Integer font;

    private JSONObject sender;

}
