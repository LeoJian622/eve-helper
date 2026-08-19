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
    @DisplayName("记录按 BATCH_SIZE 分块批量 upsert(而非逐条 insertOrUpdateSelective),不得静默丢弃")
    void saveOrUpdateBatch_callsBatchUpsert() {
        List<WalletJournal> list = List.of(new WalletJournal(), new WalletJournal());
        when(walletJournalPoConverter.domain2Po(any(WalletJournal.class)))
                .thenReturn(new WalletJournalPO());

        walletJournalRepository.saveOrUpdateBatch(list);

        // 2 条记录 < BATCH_SIZE(500) → 单批 insertOrUpdateBatch(list);逐条 insertOrUpdateSelective 已被 011 批量重构取代
        verify(walletJournalMapper, times(1)).insertOrUpdateBatch(any(List.class));
        verify(walletJournalPoConverter, times(2)).domain2Po(any(WalletJournal.class));
    }
}