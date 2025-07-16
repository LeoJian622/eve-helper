package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

/**
 * IndustryJob 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface IndustryJobAssembler {

    /**
     * IndustryJobPO 转换为 IndustryJob
     * @param industryJobPO
     * @return
     */
    IndustryJob po2Entity(IndustryJobPO industryJobPO);

    /**
     * IndustryJob 转换为 IndustryJobPO
     * @param industryJob
     * @return
     */
    IndustryJobPO entity2Po(IndustryJob industryJob);
} 