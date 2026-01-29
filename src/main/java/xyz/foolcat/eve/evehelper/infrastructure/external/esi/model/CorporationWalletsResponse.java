package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 军团钱包余额
 *
 * @author Leojan
 * date 2023-11-18 9:25
 */

@Data
@Tag(name = "军团钱包余额 200 is ok")
public class CorporationWalletsResponse {

    private Double balance;

    private Integer division;
}
