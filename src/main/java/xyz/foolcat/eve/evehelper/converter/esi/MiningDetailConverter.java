package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.system.MiningDetail;
import xyz.foolcat.eve.evehelper.esi.model.ObserverMiningLedgerResponse;

/**
 * 采矿明细转换方法
 *
 * @author Leojan
 * date 2024-07-22 17:21
 */

@Mapper(componentModel = "spring")
public interface MiningDetailConverter {

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
