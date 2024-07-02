package xyz.foolcat.eve.evehelper.onebot;

import cn.hutool.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import xyz.foolcat.eve.evehelper.onebot.model.MessageEvent;

/**
 * oneBot 工具
 *
 * @author Leojan
 * date 2024-06-25 11:32
 */

public class BotUtil {

    public static final String MESSAGE_TYPE_GROUP = "group";
    public static final String MESSAGE_TYPE_PRIVATE = "private";

    public static @NotNull JSONObject generateMessage(MessageEvent messageEvent, String message, Boolean escape) {
        return generateMessage(messageEvent.getUser_id(), messageEvent.getGroup_id(), messageEvent.getMessage_type(), message,escape);
    }

    public static @NotNull JSONObject generateMessage(Long userId, Long groupId, String messageType, String message, Boolean escape) {
        JSONObject action = new JSONObject();
        JSONObject messageBody = new JSONObject();
        messageBody.set("user_id", userId);
        messageBody.set("group_id", groupId);
        messageBody.set("message", message);
        messageBody.set("message_type", messageType);
        messageBody.set("auto_escape", escape);
        action.set("action", "send_msg");
        action.set("params", messageBody);
        return action;
    }

    public static String atSomeone(Long userId) {
        return "[CQ:at,qq=" + userId + "]";
    }
}
