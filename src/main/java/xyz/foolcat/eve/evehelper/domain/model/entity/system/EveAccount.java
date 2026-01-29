package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 游戏角色表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EveAccount extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 角色ID
     */
    private Integer characterId;

    /**
     * 角色名
     */
    private String characterName;

    /**
     * 军团（公司）ID
     */
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    private String corpName;

    /**
     * 联盟ID
     */
    private Integer allianceId;

    /**
     * 联盟名称
     */
    private String allianceName;

    /**
     * 角色授权
     */
    private String refreshToken;

    /**
     * CH:0 EU:1
     */
    private Integer type;

    /**
     * QQ号
     */
    private String qq;

    /**
     * 用户ID
     */
    private Integer userId;

    private static final long serialVersionUID = 1L;
} 