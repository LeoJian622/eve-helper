package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletTransactionPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletTransactionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletTransactionMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletTransactionRepository 实现逻辑单元测试(纯 Mockito,不依赖真实库)。
 *
 * <p>覆盖两处行为(plan A4):① {@code saveOrUpdateBatch} 逐条以复合键幂等 upsert
 * ({@code insertOrUpdateSelective}),不得静默丢弃;② {@code selectPageByOwner} 按
 * (ownerType, ownerId, division) 过滤,PO 分页内聚、映射回领域并保留 total。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransactionRepositoryImpl 仓储逻辑")
class WalletTransactionRepositoryImplTest {

    @Mock
    private WalletTransactionMapper walletTransactionMapper;
    @Mock
    private WalletTransactionPoConverter walletTransactionPoConverter;

    @InjectMocks
    private WalletTransactionRepositoryImpl walletTransactionRepository;

    @Test
    @DisplayName("saveOrUpdateBatch 分批触发 insertOrUpdateBatch(复合键幂等 upsert),不得丢弃")
    void saveOrUpdateBatch_callsBatchUpsert() {
        WalletTransaction a = new WalletTransaction();
        a.setTransactionId(1L);
        WalletTransaction b = new WalletTransaction();
        b.setTransactionId(2L);
        List<WalletTransaction> list = List.of(a, b);

        when(walletTransactionPoConverter.domain2Po(any(WalletTransaction.class)))
                .thenReturn(new WalletTransactionPO());

        walletTransactionRepository.saveOrUpdateBatch(list);

        // 2 条 < BATCH_SIZE(500),应仅调用 1 次 insertOrUpdateBatch
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WalletTransactionPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletTransactionMapper, times(1)).insertOrUpdateBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("saveOrUpdateBatch 空列表直接返回,不得触发任何 DB 调用")
    void saveOrUpdateBatch_emptyList_noOp() {
        walletTransactionRepository.saveOrUpdateBatch(List.of());
        verify(walletTransactionMapper, times(0)).insertOrUpdateBatch(anyList());
    }

    @Test
    @DisplayName("selectPageByOwner 按 ownerType/ownerId/division 过滤并映射回领域,total 保留")
    void selectPageByOwner_filtersAndMaps_preservesTotal() {
        IPage<WalletTransaction> pageIn = new Page<>(1, 10);

        WalletTransactionPO po = new WalletTransactionPO();
        po.setOwnerType("character");
        po.setOwnerId(100L);
        po.setDivision(0);
        po.setTransactionId(9L);
        po.setDate(OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        IPage<WalletTransactionPO> poPage = new Page<>(1, 10, 42);
        poPage.setRecords(List.of(po));

        when(walletTransactionMapper.selectPageByOwner(any(), eq("character"), eq(100L), eq(0)))
                .thenReturn(poPage);
        // 仓储实现调用的是 List 重载 po2Domain(List),此处 stub 该重载返回映射后的领域集合
        when(walletTransactionPoConverter.po2Domain(anyList())).thenAnswer(inv -> {
            WalletTransaction d = new WalletTransaction();
            d.setOwnerType(po.getOwnerType());
            d.setOwnerId(po.getOwnerId());
            d.setDivision(po.getDivision());
            d.setTransactionId(po.getTransactionId());
            d.setDate(po.getDate());
            return List.of(d);
        });

        IPage<WalletTransaction> result =
                walletTransactionRepository.selectPageByOwner(pageIn, "character", 100L, 0);

        verify(walletTransactionMapper).selectPageByOwner(any(), eq("character"), eq(100L), eq(0));
        assertThat(result.getTotal()).isEqualTo(42L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getTransactionId()).isEqualTo(9L);
        assertThat(result.getRecords().get(0).getDate())
                .isEqualTo(OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
    }
}