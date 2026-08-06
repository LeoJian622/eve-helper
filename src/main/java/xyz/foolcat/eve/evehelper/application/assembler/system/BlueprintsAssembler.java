package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;

import java.util.List;

/**
 * 蓝图组装器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintsAssembler {

    List<BlueprintsVO> dto2Vo(List<BlueprintsDTO> dtoList);
}
