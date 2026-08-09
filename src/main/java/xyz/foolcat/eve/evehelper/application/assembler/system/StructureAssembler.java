package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureDetailVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureFuelVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureListItemVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureServiceVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureTimerVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureFuelDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureListItemDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;

import java.util.List;

/**
 * 建筑组装器(领域读模型 -> 响应 VO)
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface StructureAssembler {

    List<StructureListItemVO> dtoList2VoList(List<StructureListItemDTO> dtoList);

    /**
     * 建筑详情领域读模型 -> 响应 VO。
     * services 字段类型不同(DTO 为 JSON 字符串,VO 为 List),由应用层解析后手动设置,此处忽略。
     */
    @Mapping(target = "services", ignore = true)
    StructureDetailVO dto2Vo(StructureDetailDTO dto);

    /**
     * 建筑燃料预警领域读模型列表 -> 响应 VO 列表
     */
    List<StructureFuelVO> fuelDtoList2VoList(List<StructureFuelDTO> dtoList);

    /**
     * 建筑服务状态领域读模型 -> 响应 VO。
     * services 字段由应用层容错解析后手动设置,此处忽略。
     */
    @Mapping(target = "services", ignore = true)
    StructureServiceVO dto2Vo(StructureServiceDTO dto);

    /**
     * 建筑时间提醒领域读模型列表 -> 响应 VO 列表
     */
    List<StructureTimerVO> timerDtoList2VoList(List<StructureTimerDTO> dtoList);
}
