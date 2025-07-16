package xyz.foolcat.eve.evehelper.shared.kernel.enums;

/**
 * 活动ID类型枚举
 *
 * @author Leojan
 * date 2024-07-08 21:19
 */

public enum CorporationActivityEnum {

    Agriculture(1,"农业"),
    Construction(2,"建设"),
    Mining(3,"采矿"),
    Chemical(4,"化工"),
    Military(5,"军事"),
    Biotechnology(6,"生物科技"),
    HighTech(7,"高科技"),
    Recreation(8,"娱乐"),
    Dockyard(9,"船坞"),
    Warehouses(10,"仓库"),
    Retail(11,"零售"),
    Trade(12,"贸易"),
    Bureaucracy(13,"官僚政治"),
    Politics(14,"政治"),
    Legal(15,"法律"),
    Security(16,"安全"),
    Finance(17,"金融"),
    Education(18,"教育"),
    Manufacturing(19,"制造"),
    Competition(20,"争夺");

    private final Integer id;

    private final String name;

    CorporationActivityEnum(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getValue(Integer id) {
        for (CorporationActivityEnum value : CorporationActivityEnum.values()) {
            if (value.id.equals(id)){
                return value.name;
            }
        }
        return null;
    }
}
