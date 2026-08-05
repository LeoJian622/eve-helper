package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

import java.util.List;


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
} 