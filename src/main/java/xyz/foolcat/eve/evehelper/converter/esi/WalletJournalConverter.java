package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.system.WalletJournal;
import xyz.foolcat.eve.evehelper.esi.model.WalletJournalResponse;

/**
 * @author Leojan
 * date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface WalletJournalConverter {

    /**
     * WalletJournalResponse 转换为 WalletJournal
     *
     * @param walletJournalResponse ESI返回的钱包记录对象
     * @return WalletJournal
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "ownerId"),
            @Mapping(source = "character", target = "character")
    })
    WalletJournal conver(WalletJournalResponse walletJournalResponse, Integer ownerId, String character);

}
