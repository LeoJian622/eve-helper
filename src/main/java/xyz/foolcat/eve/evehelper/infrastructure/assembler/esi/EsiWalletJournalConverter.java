package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;

/**
 * ESI 钱包流水响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiWalletJournalConverter {

    /**
     * WalletJournalResponse → WalletJournal
     *
     * @param character 从描述解析出的角色名(可能为空)
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "ownerId"),
            @Mapping(source = "character", target = "character")
    })
    WalletJournal toDomain(WalletJournalResponse walletJournalResponse, Integer ownerId, String character);
}