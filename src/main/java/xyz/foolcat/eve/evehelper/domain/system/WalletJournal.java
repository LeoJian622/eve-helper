package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Schema
@Data
@TableName(value = "wallet_journal")
public class WalletJournal implements Serializable {
    public static final String COL_ID = "id";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_BALANCE = "balance";
    public static final String COL_CONTEXT_ID = "context_id";
    public static final String COL_CONTEXT_ID_TYPE = "context_id_type";
    public static final String COL_DATE = "date";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_FIRST_PARTY_ID = "first_party_id";
    public static final String COL_REASON = "reason";
    public static final String COL_REF_TYPE = "ref_type";
    public static final String COL_SECOND_PARTY_ID = "second_party_id";
    public static final String COL_TAX = "tax";
    public static final String COL_TAX_RECEIVER_ID = "tax_receiver_id";
    public static final String COL_OWNER_ID = "owner_id";
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "id")
    private Long id;

    @TableField(value = "amount")
    @Schema(description = "金额")
    private Double amount;

    @TableField(value = "balance")
    @Schema(description = "账户余额")
    private Double balance;

    @TableField(value = "context_id")
    @Schema(description = "合同ID")
    private Long contextId;

    @TableField(value = "context_id_type")
    @Schema(description = "合同类型")
    private String contextIdType;

    @TableField(value = "`date`")
    @Schema(description = "日期")
    private OffsetDateTime date;

    @TableField(value = "description")
    @Schema(description = "详情")
    private String description;

    @TableField(value = "first_party_id")
    @Schema(description = "")
    private Integer firstPartyId;

    @TableField(value = "reason")
    @Schema(description = "理由")
    private String reason;

    @TableField(value = "ref_type")
    @Schema(description = "类型")
    private String refType;

    @TableField(value = "second_party_id")
    @Schema(description = "")
    private Integer secondPartyId;

    @TableField(value = "tax")
    @Schema(description = "税")
    private Double tax;

    @TableField(value = "tax_receiver_id")
    @Schema(description = "税收")
    private Integer taxReceiverId;

    @TableField(value = "`character`")
    @Schema(description = "人物")
    private String character;

    @TableField(value = "owner_id")
    @Schema(description = "所有者")
    private Long ownerId;

    private static final long serialVersionUID = 1L;
}