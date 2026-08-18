package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 钱包交易记录(ESI /wallet/transactions 领域实体)。
 * <p>
 * ownerType/ownerId/division 为归属字段,由应用/领域服务层回填
 * (ESI 响应本身不含归属信息)。
 * <p>
 * 不继承 BaseEntity:wallet_transaction 表无 gmt_create/gmt_modified 列,
 * 不需要审计时间字段。
 *
 * @author Leojan
 */
@Data
public class WalletTransaction implements Serializable {

    /**
     * 主键ID(自增,不依赖 ESI id)
     */
    private Long id;

    /**
     * 归属类型(character/corporation)
     */
    private String ownerType;

    /**
     * 归属 ID(角色 ID 或军团 ID)
     */
    private Long ownerId;

    /**
     * 军团钱包分账(军团交易有效)
     */
    private Integer division;

    /**
     * 交易 ID
     */
    private Long transactionId;

    /**
     * 交易日期
     */
    private OffsetDateTime date;

    /**
     * 物品类型 ID
     */
    private Integer typeId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private Double unitPrice;

    /**
     * 交易对手 ID
     */
    private Integer clientId;

    /**
     * 位置 ID
     */
    private Long locationId;

    /**
     * 是否买入
     */
    private Boolean isBuy;

    /**
     * 是否个人
     */
    private Boolean isPersonal;

    /**
     * 关联钱包流水 ID
     */
    private Long journalRefId;

    @Serial
    private static final long serialVersionUID = 1L;
}