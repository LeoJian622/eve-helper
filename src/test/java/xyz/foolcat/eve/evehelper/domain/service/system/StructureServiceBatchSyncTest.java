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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StructureService.batchInsertOrUpdateFromEsi 军团批量写路径落 user_id 契约测试(TDD 红-绿)。
 *
 * <p>纯 Mockito：SecurityContextHolder 设 Number principal 走请求路径 {@code authorize(cId)}，
 * ESI 军团建筑端点返回样例，断言经 {@code batchInsertOrUpdate} 落库的建筑行
 * {@code userId == eveAccount.getUserId().longValue()}（US2a T015）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StructureService.batchInsertOrUpdateFromEsi 同步者 user_id 落库")
class StructureServiceBatchSyncTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private StructureRepository structureRepository;

    @InjectMocks
    private StructureService structureService;

    @BeforeEach
    void setUpAuthenticatedRequestContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Structure structure(long structureId) {
        Structure s = new Structure();
        s.setStructureId(structureId);
        return s;
    }

    @Test
    @DisplayName("军团建筑批量：每行 setUserId=同步者 userId.longValue()")
    void corpBatch_setsUserId() throws Exception {
        Integer cId = 9001;
        Integer userId = 100;
        Integer corpId = 5001;
        String token = "Bearer corp-token";

        EveAccount account = new EveAccount();
        account.setUserId(userId);
        account.setCorpId(corpId);
        when(authorizeUtil.authorize(cId)).thenReturn(account);
        when(esiApiService.getAccessToken(cId, userId)).thenReturn(token);

        when(esiApiService.queryCorporationStructuresMaxPage(corpId, token)).thenReturn(1);
        when(esiApiService.queryCorporationStructures(corpId, "zh", 1, token))
                .thenReturn(Flux.just(structure(1001L), structure(1002L)));
        // 无待移除建筑（避免 selectByCorporationId 差异影响断言）
        when(structureRepository.selectByCorporationId(corpId)).thenReturn(List.of());

        structureService.batchInsertOrUpdateFromEsi(cId);

        Long syncUserId = userId.longValue();
        ArgumentCaptor<List<Structure>> captor = ArgumentCaptor.forClass(List.class);
        verify(structureRepository).batchInsertOrUpdate(captor.capture());

        List<Structure> saved = captor.getValue();
        assertThat(saved).hasSize(2).allSatisfy(s -> assertThat(s.getUserId()).isEqualTo(syncUserId));
    }
}