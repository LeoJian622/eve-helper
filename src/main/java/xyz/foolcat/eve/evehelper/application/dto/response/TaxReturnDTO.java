package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 退税数据对象
 *
 * @author yongj
 * date 2024-11-18 16:18
 */

@Schema(title="退税信息DTO")
@Data
public class TaxReturnDTO {

    private String name;

    private Double amount;

}
