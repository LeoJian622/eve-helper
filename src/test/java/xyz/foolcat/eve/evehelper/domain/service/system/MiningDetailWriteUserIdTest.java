package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US1 T007:采掘详情写路径落 user_id。
 *
 * <p>人物/军团观测者采掘数据同步时,把同步者 user_id 回填到每条 {@link MiningDetail} 随
 * saveOrUpdateBatch 落库(人物数据只对自己可见的关键前置)。请求路径分支见
 * {@code UserUtil.getUserId()>0}。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiningDetailService.saveObserverMining 写路径落 userId")
class MiningDetailWriteUserIdTest {

    private static final int CHARACTER_ID = 2112818290;
    private static final long OBSERVER_ID = 1014017747012L;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private UniverseNameService universeNameService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private MiningDetailRepository miningDetailRepository;

    @InjectMocks
    private MiningDetailService miningDetailService;

    @BeforeEach
    void setUpAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("同步观测者采掘后,传入 saveOrUpdateBatch 的每条记录 userId 回填为同步用户 id")
    void saveObserverMining_backfillsUserId() throws Exception {
        // ── Arrange ──
        Integer systemUserId = 55;
        Integer corpId = 98000001;
        EveAccount account = new EveAccount();
        account.setUserId(systemUserId);
        account.setCorpId(corpId);
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
        when(esiGateway.getAccessToken(CHARACTER_ID, systemUserId)).thenReturn("Bearer t");

        when(esiGateway.queryCorporationMiningObserverMaxPage(corpId, OBSERVER_ID, "Bearer t")).thenReturn(1);
        MiningDetail m1 = new MiningDetail();
        m1.setCharacterId(111);
        m1.setRecordedCorporationId(222);
        m1.setTypeId(34);
        m1.setLastUpdated(new Date());
        when(esiGateway.queryCorporationMiningObserver(eq(corpId), eq(OBSERVER_ID), eq(1), eq("Bearer t")))
                .thenReturn(Flux.just(m1));
        when(universeNameService.getUniverseName(any())).thenReturn(new HashMap<>());

        // ── Act ──
        miningDetailService.saveObserverMining(CHARACTER_ID, OBSERVER_ID);

        // ── Assert:userId 回填发生在传入 saveOrUpdateBatch 之前 ──
        ArgumentCaptor<List<MiningDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(miningDetailRepository).saveOrUpdateBatch(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        captor.getValue().forEach(d -> assertThat(d.getUserId()).isEqualTo(Long.valueOf(systemUserId)));
    }
}