package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ObserverMiningLedgerResponse;

/**
 * ESI 采矿台账响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiMiningDetailConverter {

    /**
     * ObserverMiningLedgerResponse → MiningDetail
     */
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "characterName", ignore = true),
            @Mapping(target = "recordedCorporationName", ignore = true),
            @Mapping(target = "observerId", ignore = true)
    })
    MiningDetail toDomain(ObserverMiningLedgerResponse observerMiningLedgerResponse);
}