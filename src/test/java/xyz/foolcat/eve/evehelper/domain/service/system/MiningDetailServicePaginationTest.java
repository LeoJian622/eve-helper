package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MiningDetailService 分页同步正确性测试(缺陷修复)。
 *
 * <p>发现:原 {@code Stream.iterate(1, i -> i++)} 中 {@code i++} 是后自增表达式,返回值是旧值,
 * 导致流无限生成 {@code 1,1,1,...},被 {@code limit(maxPage)} 截断后每一页都请求 page=1,
 * 永远只同步第一页数据。修复为 {@code i -> i + 1} 后应按 1..maxPage 顺序请求全部页面。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiningDetail 分页同步")
class MiningDetailServicePaginationTest {

    @Mock
    private EsiGateway esiApiService;
    @Mock
    private UniverseNameService universeNameService;
    @Mock
    private AuthorizeUtil authorizeUtil;
    @Mock
    private MiningDetailRepository miningDetailRepository;

    @InjectMocks
    private MiningDetailService miningDetailService;

    @Test
    @DisplayName("maxPage=3 时应请求 page 1,2,3 而非 1,1,1")
    void saveObserverMiningQueriesEveryPage() throws Exception {
        EveAccount account = new EveAccount();
        account.setCorpId(42);
        account.setUserId(7);
        when(authorizeUtil.authorize(100)).thenReturn(account);
        when(esiApiService.getAccessToken(100, 7)).thenReturn("token");
        when(esiApiService.queryCorporationMiningObserverMaxPage(42, 200L, "token")).thenReturn(3);

        // 按请求的 page 返回不同角色的数据,以便区分被请求的是哪一页
        when(esiApiService.queryCorporationMiningObserver(anyInt(), anyLong(), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    int page = invocation.getArgument(2);
                    MiningDetail d = new MiningDetail();
                    d.setId("p" + page);
                    d.setCharacterId(page);
                    d.setTypeId(1);
                    d.setLastUpdated(new Date());
                    return Flux.just(d);
                });
        // 用 HashMap 代替 Map.of():后者是不可变 map,get(null) 会抛 NPE,而受测逻辑会以 null 键查询
        when(universeNameService.getUniverseName(any())).thenReturn(new java.util.HashMap<>());
        when(miningDetailRepository.saveOrUpdateBatch(any())).thenReturn(1);

        miningDetailService.saveObserverMining(100, 200L);

        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(esiApiService, times(3)).queryCorporationMiningObserver(eq(42), eq(200L), pageCaptor.capture(), anyString());
        assertThat(pageCaptor.getAllValues())
                .as("分页流必须以 1..maxPage 递增请求,而非重复 page=1")
                .containsExactly(1, 2, 3);
    }
}