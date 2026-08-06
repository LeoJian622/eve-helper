package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;

import java.util.List;

/**
 * MiningDetail 领域实体与 MiningDetailPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MiningDetailPoConverter {

    /**
     * MiningDetailPO 转换为 MiningDetail
     * @param miningDetailPO
     * @return
     */
    MiningDetail po2Domain(MiningDetailPO miningDetailPO);

    /**
     * MiningDetail 转换为 MiningDetailPO
     * @param miningDetail
     * @return
     */
    MiningDetailPO domain2Po(MiningDetail miningDetail);

    /**
     * List<MiningDetailPO> 转换为 List<MiningDetail>
     * @param miningDetailPOList
     * @return
     */
    List<MiningDetail> po2Domain(List<MiningDetailPO> miningDetailPOList);

    /**
     * List<MiningDetail> 转换为 List<MiningDetailPO>
     * @param miningDetailList
     * @return
     */
    List<MiningDetailPO> domain2Po(List<MiningDetail> miningDetailList);
}
