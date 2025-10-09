package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

import java.util.List;

;

/**
 * 钱包日志转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletJournalAssembler {

    /**
     * WalletJournalPO 转换为 WalletJournal
     * @param walletJournalPO
     * @return
     */
    WalletJournal po2Domain(WalletJournalPO walletJournalPO);

    /**
     * WalletJournal 转换为 WalletJournalPO
     * @param walletJournal
     * @return
     */
    WalletJournalPO domain2Po(WalletJournal walletJournal);

    List<WalletJournal> po2Domain(List<WalletJournalPO> walletJournalPOList);

    List<WalletJournalPO> domain2Po(List<WalletJournal> walletJournalList);

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
    WalletJournal toWalletJournal(WalletJournalResponse walletJournalResponse, Integer ownerId, String character);
} 