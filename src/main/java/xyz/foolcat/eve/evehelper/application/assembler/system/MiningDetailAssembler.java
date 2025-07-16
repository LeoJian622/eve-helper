package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;

;

/**
 * 采矿详情转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MiningDetailAssembler {

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
} 