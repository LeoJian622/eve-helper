package xyz.foolcat.eve.evehelper.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户角色列表
 *
 * @author Leojan
 * date 2026-07-31 16:20
 */

@Schema(description="用户角色列表")
@Data
public class UserAccountDTO {
    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private Integer characterId;

    /**
     * 角色名
     */
    @Schema(description = "角色名")
    private String characterName;

    /**
     * 军团（公司）ID
     */
    @Schema(description = "军团（公司）ID")
    private Integer corpId;

    /**
     * 军团（公司）名称
     */
    @Schema(description = "军团（公司）名称")
    private String corpName;

    /**
     * 联盟ID
     */
    @Schema(description = "联盟ID")
    private Integer allianceId;

    /**
     * 联盟名称
     */
    @Schema(description = "联盟名称")
    private String allianceName;
}
