package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

import java.util.List;

/**
 * WalletJournal 领域实体与 WalletJournalPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletJournalPoConverter {

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
