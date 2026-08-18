package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 钱包交易记录表(wallet_transaction)持久化对象。
 *
 * <p>归属列 owner_type/owner_id/division 与 ESI 的 transaction_id 构成幂等复合唯一键
 * (UNIQUE uk_owner_div_tx)。date 为 MySQL 保留字,列定义须反引号。</p>
 *
 * @author Leojan
 */
@Schema(description = "钱包交易记录表")
@Data
@TableName(value = "wallet_transaction")
public class WalletTransactionPO implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField(value = "owner_type")
    @Schema(description = "所有者类型")
    private String ownerType;
    @TableField(value = "owner_id")
    @Schema(description = "所有者ID")
    private Long ownerId;
    @TableField(value = "division")
    @Schema(description = "钱包 division")
    private Integer division;
    @TableField(value = "transaction_id")
    @Schema(description = "交易ID")
    private Long transactionId;
    @TableField(value = "`date`")
    @Schema(description = "交易日期")
    private OffsetDateTime date;
    @TableField(value = "type_id")
    @Schema(description = "物品类型ID")
    private Integer typeId;
    @TableField(value = "quantity")
    @Schema(description = "数量")
    private Integer quantity;
    @TableField(value = "unit_price")
    @Schema(description = "单价")
    private Double unitPrice;
    @TableField(value = "client_id")
    @Schema(description = "客户端ID")
    private Integer clientId;
    @TableField(value = "location_id")
    @Schema(description = "位置ID")
    private Long locationId;
    @TableField(value = "is_buy")
    @Schema(description = "是否买单")
    private Boolean isBuy;
    @TableField(value = "is_personal")
    @Schema(description = "是否个人交易")
    private Boolean isPersonal;
    @TableField(value = "journal_ref_id")
    @Schema(description = "关联钱包流水ID")
    private Long journalRefId;
    @Serial
    private static final long serialVersionUID = 1L;
}