package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public record QQBotMessageEvent(
        String messageId,
        String authorOpenid,
        String groupOpenid,
        String content,
        Scene scene
) {
    public enum Scene {
        GROUP,
        C2C
    }

    public static Optional<QQBotMessageEvent> fromGateway(String eventType, JsonNode data) {
        if (data == null || data.isNull()) {
            return Optional.empty();
        }

        Scene scene;
        String authorOpenid;
        String groupOpenid = null;
        switch (eventType) {
            case "GROUP_AT_MESSAGE_CREATE", "GROUP_MESSAGE_CREATE" -> {
                scene = Scene.GROUP;
                authorOpenid = text(data.path("author"), "member_openid", "id");
                groupOpenid = text(data, "group_openid");
            }
            case "C2C_MESSAGE_CREATE" -> {
                scene = Scene.C2C;
                authorOpenid = text(data.path("author"), "user_openid", "id");
            }
            default -> {
                return Optional.empty();
            }
        }

        String messageId = text(data, "id");
        if (messageId == null || authorOpenid == null || (scene == Scene.GROUP && groupOpenid == null)) {
            return Optional.empty();
        }
        return Optional.of(new QQBotMessageEvent(
                messageId,
                authorOpenid,
                groupOpenid,
                data.path("content").asText("").trim(),
                scene
        ));
    }

    private static String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
