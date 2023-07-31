package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema
@Data
@TableName(value = "corporation_wallet_journal")
public class CorporationWalletJournal implements Serializable {
    @TableId(value = "id")
    @Schema(description = "")
    private Integer id;

    /**
     * 金额
     */
    @TableField(value = "amount")
    @Schema(description = "金额")
    private Double amount;

    /**
     * 余额
     */
    @TableField(value = "balance")
    @Schema(description = "余额")
    private Double balance;

    @TableField(value = "context_id")
    @Schema(description = "")
    private Integer contextId;

    @TableField(value = "context_id_type")
    @Schema(description = "")
    private Integer contextIdType;

    @TableField(value = "`date`")
    @Schema(description = "")
    private Date date;

    @TableField(value = "description")
    @Schema(description = "")
    private String description;

    @TableField(value = "first_party_id")
    @Schema(description = "")
    private Integer firstPartyId;

    @TableField(value = "reason")
    @Schema(description = "")
    private String reason;

    /**
     * 交易类型
     */
    @TableField(value = "ref_type")
    @Schema(description = "交易类型")
    private String refType;

    @TableField(value = "second_party_id")
    @Schema(description = "")
    private Integer secondPartyId;

    @TableField(value = "tax")
    @Schema(description = "")
    private Integer tax;

    @TableField(value = "tax_receiver_id")
    @Schema(description = "")
    private Integer taxReceiverId;

    private static final long serialVersionUID = 1L;

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
}