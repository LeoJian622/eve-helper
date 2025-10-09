package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ObserverMiningLedgerResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;

import java.util.List;

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

    /**
     * ObserverMiningLedgerResponse 转换为 MiningDetail
     * @param observerMiningLedgerResponse ESI返回的采矿明细对象
     * @return MiningDetail
     */
    @Mappings({
            @Mapping(target = "id",ignore = true),
            @Mapping(target = "characterName",ignore = true),
            @Mapping(target = "recordedCorporationName",ignore = true),
            @Mapping(target = "observerId",ignore = true),
    })
    MiningDetail toMiningDetail(ObserverMiningLedgerResponse observerMiningLedgerResponse);
} 