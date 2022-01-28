package xyz.foolcat.eve.evehelper.dto.esi;

import lombok.Data;

/**
 * item信息查询接收体
 *
 * @author Leojan
 * @date 2021-12-14 11:12
 */

@Data
public class UniverseNameResponeDTO {

    private Integer id;

    private String name;

    private String category;
}
