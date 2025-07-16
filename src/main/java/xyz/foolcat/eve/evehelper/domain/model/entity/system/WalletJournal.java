package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 钱包日志表
 * @author Leojan
 */
@Data
public class WalletJournal  implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 金额
     */
    private Double amount;

    /**
     * 账户余额
     */
    private Double balance;

    /**
     * 合同ID
     */
    private Long contextId;

    /**
     * 合同类型
     */
    private String contextIdType;

    /**
     * 日期
     */
    private OffsetDateTime date;

    /**
     * 详情
     */
    private String description;

    /**
     * 第一方ID
     */
    private Long firstPartyId;

    /**
     * 理由
     */
    private String reason;

    /**
     * 类型
     */
    private String refType;

    /**
     * 第二方ID
     */
    private Long secondPartyId;

    /**
     * 税
     */
    private Double tax;

    /**
     * 税收接收者ID
     */
    private Long taxReceiverId;

    /**
     * 人物
     */
    private String character;

    /**
     * 所有者ID
     */
    private Long ownerId;

    private static final long serialVersionUID = 1L;
} 