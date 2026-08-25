package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 游戏角色表
 *
 * <p><b>安全</b>：{@code toString()} 排除 {@code refreshToken}（2026-08-25 安全审计 LOW-2）。
 * {@code queryAccountList} 会 SELECT {@code refresh_token}，故服务层持有的每个实例都携带
 * （加密的）refreshToken；一旦有人写出 {@code log.warn("accounts={}", accounts)} 就会整批落盘。
 * {@code SecurityBaselineValidator} 只能在生产拦住 debug/trace 级别，拦不住 warn，
 * 故在实体层面从根上排除。</p>
 *
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = "refreshToken")
public class EveAccount extends BaseEntity implements Serializable {

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

    @Serial
    private static final long serialVersionUID = 1L;
} 