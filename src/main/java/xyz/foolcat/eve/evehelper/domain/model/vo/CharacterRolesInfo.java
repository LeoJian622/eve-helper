package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 角色军团权限信息(ESI /characters/{id}/roles/ 响应映射)。
 * <p>
 * 领域层读模型,供伴侣 APP 建筑查询、团员追踪、军团工业等功能的权限判定。
 * 三个集合分别对应 ESI roles_at_hq / roles_at_base / roles_at_other。
 * <p>
 * 权限判定助手以精确角色字符串常量钉死判定规则(对照 ESI corp job-role 文档)。
 *
 * @param rolesAtHq    总部角色集合
 * @param rolesAtBase  基地角色集合
 * @param rolesAtOther 其他角色集合
 * @author Leojan
 */
public record CharacterRolesInfo(
        List<String> rolesAtHq,
        List<String> rolesAtBase,
        List<String> rolesAtOther
) {

    /** ESI 角色常量:Director(总监) */
    public static final String ROLE_DIRECTOR = "Director";

    /** ESI 角色常量:Station_Manager(建筑管理员) */
    public static final String ROLE_STATION_MANAGER = "Station_Manager";

    /** ESI 角色常量:Accountant(会计,军团工业订单权限) */
    public static final String ROLE_ACCOUNTANT = "Accountant";

    /**
     * 建筑接口权限判定(T030/T031 消费)。
     * <p>
     * 精确集合:{Director, Station_Manager}——三集合任一含其一即有权。
     *
     * @return true 表示有权操作军团建筑
     */
    public boolean hasStructurePermission() {
        return containsAny(ROLE_DIRECTOR, ROLE_STATION_MANAGER);
    }

    /**
     * 团员追踪权限判定(T038 消费)。
     * <p>
     * 精确集合:{Director}——仅 Director 满足,Station_Manager 单独不足。
     *
     * @return true 表示有权追踪军团成员
     */
    public boolean hasDirectorRole() {
        return containsAny(ROLE_DIRECTOR);
    }

    /**
     * 军团工业订单权限判定(T022 消费)。
     * <p>
     * 精确集合:{Director, Accountant}——ESI corp industry jobs 端点要求此二角色之一。
     *
     * @return true 表示有权查看军团工业订单
     */
    public boolean hasIndustryPermission() {
        return containsAny(ROLE_DIRECTOR, ROLE_ACCOUNTANT);
    }

    /**
     * 三集合任一含指定角色之一。
     */
    private boolean containsAny(String... roles) {
        for (String role : roles) {
            if (rolesAtHq.contains(role) || rolesAtBase.contains(role) || rolesAtOther.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
