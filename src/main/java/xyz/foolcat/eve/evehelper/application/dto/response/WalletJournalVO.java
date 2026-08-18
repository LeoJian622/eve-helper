package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 人物钱包流水出参
 *
 * @author Leojan
 */
@Schema(description = "人物钱包流水")
@Data
public class WalletJournalVO implements Serializable {

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 金额
     */
    @Schema(description = "金额")
    private Double amount;

    /**
     * 账户余额
     */
    @Schema(description = "账户余额")
    private Double balance;

    /**
     * 日期
     */
    @Schema(description = "日期")
    private OffsetDateTime date;

    /**
     * 类型
     */
    @Schema(description = "类型")
    private String refType;

    /**
     * 详情
     */
    @Schema(description = "详情")
    private String description;

    /**
     * 税
     */
    @Schema(description = "税")
    private Double tax;

    /**
     * 所有者ID
     */
    @Schema(description = "所有者ID")
    private Long ownerId;

    @Schema(description = "分账(人物=0,军团=1-7)")
    private Integer division;

    @Serial
    private static final long serialVersionUID = 1L;
}