package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IndustryJobQueryApplicationService 单元测试(TDD RED, 022-T021)。
 *
 * <p>纯 Mockito 单测,固化 US3 工业直查应用服务的 7 个行为契约(AC 6 + 控制器追加 1)。
 * 本任务仅写失败测试——IndustryJobQueryApplicationService 与 IndustryJobPageResponse DTO
 * 由 T022 创建,编译失败本身即 RED 证据。</p>
 *
 * <p>用例:
 * <ol>
 *   <li>charJobs_returnsAllStatuses_whenStatusNull</li>
 *   <li>charJobs_filtersByStatus</li>
 *   <li>charJobs_empty_returnsEmptyListNotError</li>
 *   <li>charJobs_pagination_slicesInMemory</li>
 *   <li>corpJobs_requiresCorpRole</li>
 *   <li>otherUsersCharacter_rejected</li>
 *   <li>corpJobs_usesAccountCorpId</li>
 * </ol>
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryJobQueryApplicationService 工业直查应用服务(022 US3)")
class IndustryJobQueryApplicationServiceTest {

    private static final int CHARACTER_ID = 95465499;
    private static final int CORP_ID = 1000001;
    private static final int USER_ID = 1;
    private static final String STUB_ACCESS_TOKEN = "Bearer stub-industry-token";

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EsiGateway esiGateway;

    // ── 辅助方法 ──

    private EveAccount buildAccount() {
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setCharacterName("IndustryTestChar");
        account.setUserId(USER_ID);
        account.setCorpId(CORP_ID);
        return account;
    }

    private void stubAuthorize() {
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(buildAccount());
    }

    private void stubAccessToken() throws ParseException {
        when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID))
                .thenReturn(new CharacterAccessTokenResult(STUB_ACCESS_TOKEN, CHARACTER_ID, 1140L));
    }

    private IndustryJob buildJob(Long jobId, String status) {
        IndustryJob job = new IndustryJob();
        job.setJobId(jobId);
        job.setActivityId(1);
        job.setStatus(status);
        job.setProductTypeId(587);
        job.setRuns(10);
        job.setStartDate(OffsetDateTime.parse("2026-11-10T08:00:00Z"));
        job.setEndDate(OffsetDateTime.parse("2026-11-11T08:00:00Z"));
        job.setInstallerId((long) CHARACTER_ID);
        job.setFacilityId(60003760L);
        return job;
    }

    /* ============================ US3 角色工业 ============================ */

    @Test
    @DisplayName("charJobs status=null 返回全量状态(不过滤)")
    void charJobs_returnsAllStatuses_whenStatusNull() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.queryCharacterIndustryJobs(eq(CHARACTER_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false)))
                .thenReturn(Flux.just(
                        buildJob(1L, IndustryJob.STATUS_ACTIVE),
                        buildJob(2L, IndustryJob.STATUS_DELIVERED),
                        buildJob(3L, IndustryJob.STATUS_ACTIVE)
                ));

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        IndustryJobPageResponse result = service.queryJobs(
                "char", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false);

        assertThat(result.jobs()).hasSize(3);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("charJobs status=active 仅返回 active 状态")
    void charJobs_filtersByStatus() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.queryCharacterIndustryJobs(eq(CHARACTER_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false)))
                .thenReturn(Flux.just(
                        buildJob(1L, IndustryJob.STATUS_ACTIVE),
                        buildJob(2L, IndustryJob.STATUS_DELIVERED),
                        buildJob(3L, IndustryJob.STATUS_ACTIVE)
                ));

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        IndustryJobPageResponse result = service.queryJobs(
                "char", CHARACTER_ID, "active",
                PageQuery.builder().current(1).size(20).build(), false);

        assertThat(result.jobs()).hasSize(2);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.jobs()).allSatisfy(item ->
                assertThat(item.status()).isEqualTo(IndustryJob.STATUS_ACTIVE));
    }

    @Test
    @DisplayName("charJobs Flux.empty 返回空列表而非异常")
    void charJobs_empty_returnsEmptyListNotError() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.queryCharacterIndustryJobs(eq(CHARACTER_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false)))
                .thenReturn(Flux.empty());

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        IndustryJobPageResponse result = service.queryJobs(
                "char", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false);

        assertThat(result.jobs()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("charJobs 内存分页:5 条 current=2/size=2 返回第 3-4 条,total=5")
    void charJobs_pagination_slicesInMemory() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.queryCharacterIndustryJobs(eq(CHARACTER_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false)))
                .thenReturn(Flux.just(
                        buildJob(1L, IndustryJob.STATUS_ACTIVE),
                        buildJob(2L, IndustryJob.STATUS_ACTIVE),
                        buildJob(3L, IndustryJob.STATUS_ACTIVE),
                        buildJob(4L, IndustryJob.STATUS_ACTIVE),
                        buildJob(5L, IndustryJob.STATUS_ACTIVE)
                ));

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        IndustryJobPageResponse result = service.queryJobs(
                "char", CHARACTER_ID, null,
                PageQuery.builder().current(2).size(2).build(), false);

        assertThat(result.jobs()).hasSize(2);
        assertThat(result.total()).isEqualTo(5);
        // 第 2 页 = 跳过前 2 条,取第 3-4 条
        assertThat(result.jobs().get(0).jobId()).isEqualTo(3L);
        assertThat(result.jobs().get(1).jobId()).isEqualTo(4L);
    }

    /* ============================ US3 军团工业 ============================ */

    @Test
    @DisplayName("corpJobs 无 Director/Accountant 角色抛出权限异常且工业网关零调用")
    void corpJobs_requiresCorpRole() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        // 三集合均无 Director/Accountant
        CharacterRolesInfo emptyRoles = new CharacterRolesInfo(List.of(), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN)))
                .thenReturn(emptyRoles);

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        assertThatThrownBy(() -> service.queryJobs(
                "corp", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false))
                .isInstanceOf(EsiAccessException.class)
                .hasMessage(EsiAccessException.MSG_PERMISSION_LOW);

        // 工业查询网关零调用
        verify(esiGateway, never()).queryCorporationIndustryJobs(anyInt(), anyBoolean(), any(), anyBoolean());
    }

    // ── MIN-A: roles=null 不 NPE，走需重新授权语义 ──

    @Test
    @DisplayName("corpJobs roles=null 不 NPE，抛 EsiAccessException.permissionLow（MIN-A）")
    void corpJobs_rolesNull_throwsPermissionLow() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        // 网关返回 null（角色权限查询失败/未授权场景）
        when(esiGateway.queryCharacterRoles(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN)))
                .thenReturn(null);

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        assertThatThrownBy(() -> service.queryJobs(
                "corp", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false))
                .isInstanceOf(EsiAccessException.class)
                .hasMessage(EsiAccessException.MSG_PERMISSION_LOW);

        verify(esiGateway, never()).queryCorporationIndustryJobs(anyInt(), anyBoolean(), any(), anyBoolean());
    }

    /* ============================ US3 归属安全 ============================ */

    @Test
    @DisplayName("otherUsersCharacter authorize 抛 ACCESS_UNAUTHORIZED 透传且网关零调用")
    void otherUsersCharacter_rejected() throws ParseException {
        when(authorizeUtil.authorize(CHARACTER_ID))
                .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        assertThatThrownBy(() -> service.queryJobs(
                "char", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false))
                .isInstanceOf(EveHelperException.class)
                .satisfies(ex -> assertThat(((EveHelperException) ex).getResultCode())
                        .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));

        // 网关零调用:越权拒绝发生在取数之前
        verify(esiGateway, never()).getAccessTokenWithExpiry(anyInt(), anyInt());
        verify(esiGateway, never()).queryCharacterIndustryJobs(anyInt(), anyBoolean(), any(), anyBoolean());
    }

    /* ============================ US3 军团 corpId 派生(控制器追加) ============================ */

    @Test
    @DisplayName("corpJobs 使用 account.getCorpId() 而非路径 id 查询军团工业")
    void corpJobs_usesAccountCorpId() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        // roles 含 Director → hasIndustryPermission() = true
        CharacterRolesInfo directorRoles = new CharacterRolesInfo(
                List.of(CharacterRolesInfo.ROLE_DIRECTOR), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN)))
                .thenReturn(directorRoles);

        IndustryJob corpJob = buildJob(100L, IndustryJob.STATUS_ACTIVE);
        corpJob.setCorporationId(CORP_ID);

        when(esiGateway.queryCorporationIndustryJobs(eq(CORP_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false)))
                .thenReturn(Flux.just(corpJob));

        IndustryJobQueryApplicationService service =
                new IndustryJobQueryApplicationService(authorizeUtil, esiGateway);

        IndustryJobPageResponse result = service.queryJobs(
                "corp", CHARACTER_ID, null,
                PageQuery.builder().current(1).size(20).build(), false);

        assertThat(result.jobs()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        // verify 网关收到的是 account.getCorpId() 而非路径传入的 CHARACTER_ID
        verify(esiGateway).queryCorporationIndustryJobs(eq(CORP_ID), eq(true),
                eq(STUB_ACCESS_TOKEN), eq(false));
    }
}
