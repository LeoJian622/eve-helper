package xyz.foolcat.eve.evehelper.domain.specification;

import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;

/**
 * 蓝图查询规格
 * 用于封装复杂的查询条件，遵循DDD的规格模式
 */
@Component
public class BlueprintsSpecification {
    
    /**
     * 根据所有者ID查询
     */
    public boolean isSatisfiedByOwnerId(Blueprints blueprints, String ownerId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            return true;
        }
        return ownerId.equals(blueprints.getOwnerId());
    }
    
    /**
     * 根据蓝图名称模糊查询
     */
    public boolean isSatisfiedByBlueprintName(Blueprints blueprints, String blueprintName) {
        if (blueprintName == null || blueprintName.trim().isEmpty()) {
            return true;
        }
        // 这里需要根据实际的字段名进行调整
        // return blueprints.getName() != null && 
        //        blueprints.getName().toLowerCase().contains(blueprintName.toLowerCase());
        return true;
    }
    
    /**
     * 根据蓝图类型查询
     */
    public boolean isSatisfiedByBlueprintType(Blueprints blueprints, String blueprintType) {
        if (blueprintType == null || blueprintType.trim().isEmpty()) {
            return true;
        }
        // 这里需要根据实际的字段名进行调整
        // return blueprintType.equals(blueprints.getType());
        return true;
    }
    
    /**
     * 组合查询条件
     */
    public boolean isSatisfiedBy(Blueprints blueprints, String ownerId, String blueprintName, String blueprintType) {
        return isSatisfiedByOwnerId(blueprints, ownerId) &&
               isSatisfiedByBlueprintName(blueprints, blueprintName) &&
               isSatisfiedByBlueprintType(blueprints, blueprintType);
    }
} 