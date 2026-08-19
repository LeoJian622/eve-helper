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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.IndustryJobRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IndustryJobService.batchInsertOrUpdateFromEsi 批量写路径落 user_id 契约测试(TDD 红-绿)。
 *
 * <p>纯 Mockito：SecurityContextHolder 设 Number principal 走请求路径 {@code authorize(cid)}，
 * 军团生产线端点(isCor=true)返回样例，断言经 {@code batchInsertOrUpdate} 落库的作业行
 * {@code userId == eveAccount.getUserId().longValue()}（US2a T016）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryJobService.batchInsertOrUpdateFromEsi 同步者 user_id 落库")
class IndustryJobServiceBatchSyncTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private InvTypesService invTypesService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private IndustryJobRepository industryJobRepository;

    @InjectMocks
    private IndustryJobService industryJobService;

    @BeforeEach
    void setUpAuthenticatedRequestContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static IndustryJob job(long jobId) {
        IndustryJob j = new IndustryJob();
        j.setJobId(jobId);
        j.setBlueprintTypeId(1);
        j.setProductTypeId(2);
        j.setActivityId(1);
        return j;
    }

    @Test
    @DisplayName("军团生产线批量：每行 setUserId=同步者 userId.longValue()")
    void corpBatch_setsUserId() throws Exception {
        Integer cid = 9001;
        Integer userId = 100;
        Integer corpId = 5001;
        String token = "Bearer corp-token";

        EveAccount account = new EveAccount();
        account.setUserId(userId);
        account.setCorpId(corpId);
        when(authorizeUtil.authorize(cid)).thenReturn(account);
        when(esiApiService.getAccessToken(cid, userId)).thenReturn(token);

        when(esiApiService.queryCorporationIndustryJobsMaxPage(corpId, true, token)).thenReturn(1);
        when(esiApiService.queryCorporationIndustryJobs(corpId, true, token))
                .thenReturn(Flux.just(job(1001L), job(1002L)));
        when(invTypesService.getNameByTypeIds(anyList())).thenReturn(Map.of(1, "蓝图", 2, "产物"));

        industryJobService.batchInsertOrUpdateFromEsi(cid, true, true);

        Long syncUserId = userId.longValue();
        ArgumentCaptor<List<IndustryJob>> captor = ArgumentCaptor.forClass(List.class);
        verify(industryJobRepository).batchInsertOrUpdate(captor.capture());

        List<IndustryJob> saved = captor.getValue();
        assertThat(saved).hasSize(2).allSatisfy(j -> assertThat(j.getUserId()).isEqualTo(syncUserId));
    }
}