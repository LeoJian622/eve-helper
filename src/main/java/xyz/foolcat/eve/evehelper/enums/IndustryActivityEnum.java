package xyz.foolcat.eve.evehelper.enums;

/**
 * 活动ID类型枚举
 *
 * @author Leojan
 * date 2024-07-08 21:19
 */

public enum IndustryActivityEnum {

    ACTIVITY_NONE(0,"None"),
    ACTIVITY_MANUFACTURING(1,"制造"),
    ACTIVITY_RESEARCHING_TECHNOLOGY(2,"研究技术"),
    ACTIVITY_RESEARCHING_TIME_PRODUCTIVITY(3,"研究时间生产率"),
    ACTIVITY_RESEARCHING_MATERIAL_PRODUCTIVITY(4,"研究材料生产率"),
    ACTIVITY_COPYING(5,"拷贝"),
    ACTIVITY_DUPLICATING(6,"Duplicating"),
    ACTIVITY_REVERSE_ENGINEERING(7,"逆向工程"),
    ACTIVITY_REACTIONS(8,"发明"),
    ACTIVITY_REACTIONS_T2(9,"复合反应"),
    ACTIVITY_REACTIONS_T3(11,"生化反应"),
    ACTIVITY_UNKNOWN(-1,"未知");

    private final Integer id;

    private final String name;

    IndustryActivityEnum(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getValue(Integer id) {
        for (IndustryActivityEnum value : IndustryActivityEnum.values()) {
            if (value.id.equals(id)){
                return value.name;
            }
        }
        return null;
    }
}
