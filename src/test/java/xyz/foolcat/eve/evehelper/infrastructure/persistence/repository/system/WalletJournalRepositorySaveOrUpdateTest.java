package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletJournalPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletJournalRepository.saveOrUpdateBatch 落库正确性测试(缺陷修复)。
 *
 * <p>发现:原实现为空方法体,而 {@link WalletJournalService} 同步钱包日志时调用它,
 * 导致查询到的日志被静默丢弃、永不落库。修复为循环调用 {@code insertOrUpdateSelective}
 * (其 SQL 为 {@code insert ... on duplicate key update},正是幂等 upsert 语义)。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournal saveOrUpdateBatch 落库")
class WalletJournalRepositorySaveOrUpdateTest {

    @Mock
    private WalletJournalMapper walletJournalMapper;
    @Mock
    private WalletJournalPoConverter walletJournalPoConverter;

    @InjectMocks
    private WalletJournalRepositoryImpl walletJournalRepository;

    @Test
    @DisplayName("每笔记录都应触发单条 upsert,不得静默丢弃")
    void saveOrUpdateBatch_callsUpsertForEachRecord() {
        List<WalletJournal> list = List.of(new WalletJournal(), new WalletJournal());
        when(walletJournalPoConverter.domain2Po(any(WalletJournal.class)))
                .thenReturn(new WalletJournalPO());

        walletJournalRepository.saveOrUpdateBatch(list);

        verify(walletJournalMapper, times(2)).insertOrUpdateSelective(any(WalletJournalPO.class));
    }
}