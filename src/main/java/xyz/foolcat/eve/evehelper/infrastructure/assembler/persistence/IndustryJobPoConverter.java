package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

import java.util.List;

/**
 * IndustryJob 领域实体与 IndustryJobPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface IndustryJobPoConverter {

    /**
     * IndustryJobPO 转换为 IndustryJob
     * @param industryJobPO
     * @return
     */
    IndustryJob po2Domain(IndustryJobPO industryJobPO);

    /**
     * IndustryJob 转换为 IndustryJobPO
     * @param industryJob
     * @return
     */
    IndustryJobPO domain2Po(IndustryJob industryJob);

    /**
     * IndustryJobPO 列表转换为 IndustryJob 列表
     * @param industryJobPOs
     * @return
     */
    List<IndustryJob> po2Domain(List<IndustryJobPO> industryJobPOs);

    /**
     * IndustryJob 列表转换为 IndustryJobPO 列表
     * @param industryJobs
     * @return
     */
    List<IndustryJobPO> domain2Po(List<IndustryJob> industryJobs);
}
