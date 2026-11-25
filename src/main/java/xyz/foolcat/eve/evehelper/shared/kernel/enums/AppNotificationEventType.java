package xyz.foolcat.eve.evehelper.shared.kernel.enums;

/**
 * APP 通知事件类型枚举
 * <p>
 * 用于标识伴侣 APP 推送通知的业务事件语义。
 * <ul>
 *     <li>INDUSTRY_COMPLETE:工业任务完成(蓝图制造/研究结束)</li>
 *     <li>FUEL_LOW:建筑燃料不足(燃料即将耗尽预警)</li>
 *     <li>SKILL_QUEUE_EMPTY:技能队列为空(角色训练队列已结束)</li>
 * </ul>
 *
 * @author Leojan
 * date 2026-11-12
 */
public enum AppNotificationEventType {

    INDUSTRY_COMPLETE("工业任务完成"),
    FUEL_LOW("建筑燃料不足"),
    SKILL_QUEUE_EMPTY("技能队列为空");

    private final String description;

    AppNotificationEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
