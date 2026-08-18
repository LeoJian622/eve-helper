package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 钱包交易出参
 *
 * @author Leojan
 */
@Schema(description = "钱包交易")
@Data
public class WalletTransactionVO implements Serializable {

    /**
     * 交易日期
     */
    @Schema(description = "交易日期")
    private OffsetDateTime date;

    /**
     * 物品类型 ID
     */
    @Schema(description = "物品类型ID")
    private Integer typeId;

    /**
     * 数量
     */
    @Schema(description = "数量")
    private Integer quantity;

    /**
     * 单价
     */
    @Schema(description = "单价")
    private Double unitPrice;

    /**
     * 是否买入
     */
    @Schema(description = "是否买入")
    private Boolean isBuy;

    /**
     * 交易对手 ID
     */
    @Schema(description = "交易对手ID")
    private Integer clientId;

    /**
     * 位置 ID
     */
    @Schema(description = "位置ID")
    private Long locationId;

    /**
     * 关联钱包流水 ID
     */
    @Schema(description = "关联钱包流水ID")
    private Long journalRefId;

    /**
     * 归属类型(character/corporation)
     */
    @Schema(description = "归属类型")
    private String ownerType;

    /**
     * 归属 ID(角色 ID 或军团 ID)
     */
    @Schema(description = "归属ID")
    private Long ownerId;

    /**
     * 军团钱包分账(军团交易有效)
     */
    @Schema(description = "军团钱包分账")
    private Integer division;

    /**
     * 交易 ID
     */
    @Schema(description = "交易ID")
    private Long transactionId;

    @Serial
    private static final long serialVersionUID = 1L;
}