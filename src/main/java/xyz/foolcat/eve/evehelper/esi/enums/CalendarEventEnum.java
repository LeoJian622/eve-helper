package xyz.foolcat.eve.evehelper.esi.enums;

/**
 * 日历事件枚举
 *
 * @author Leojan
 * @date 2023-10-18 14:58
 */

public enum CalendarEventEnum {

    /**
     * 事件回复枚举
     */
    ACCEPTED("accepted")
    , DECLINED("declined")
    , TENTATIVE("tentative");

    private String value;

    CalendarEventEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
