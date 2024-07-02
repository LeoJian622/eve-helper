package xyz.foolcat.eve.evehelper.dto.esi;

import lombok.Data;

/**
 * 人物信息查询对象
 *
 * @author Leojan
 * date 2021-12-13 16:29
 */
@Data
@Deprecated
public class CharacterInfoResponseDTO {

    private Integer alliance_id;

    private Integer corporation_id;

}
