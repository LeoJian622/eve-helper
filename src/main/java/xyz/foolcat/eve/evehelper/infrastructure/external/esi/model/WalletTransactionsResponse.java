package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 钱包交易记录
 *
 * @author Leojan
 * date 2023-11-17 23:13
 */

@Data
@Tag(name = "钱包交易记录 200 is ok")
public class WalletTransactionsResponse{

	private String date;

	@JsonProperty("transaction_id")
	private Long transactionId;

	private Integer quantity;

	@JsonProperty("type_id")
	private Integer typeId;

	@JsonProperty("unit_price")
	private Double unitPrice;

	@JsonProperty("client_id")
	private Integer clientId;

	@JsonProperty("location_id")
	private Long locationId;

	@JsonProperty("is_buy")
	private Boolean isBuy;

	@JsonProperty("is_personal")
	private Boolean isPersonal;

	@JsonProperty("journal_ref_id")
	private Long journalRefId;
}