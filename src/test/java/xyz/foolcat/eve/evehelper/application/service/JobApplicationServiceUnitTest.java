package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.IndustryJobService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工业制造应用服务单元测试。
 * <p>
 * 除编排逻辑外，重点验证归属校验（IDOR 防御）：该接口按用户可控的 id
 * 同步 ESI 数据，须先确认该人物/军团属于当前用户。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("工业制造应用服务单元测试")
class JobApplicationServiceUnitTest {

    private static final Integer CURRENT_USER_ID = 5;

    private static final Integer OWN_CHARACTER_ID = 2112818290;

    private static final Integer OWN_CORP_ID = 98000001;

    private static final Integer OTHER_CHARACTER_ID = 90000001;

    @MockBean
    IndustryJobService industryJobService;

    @MockBean
    EveAccountService eveAccountService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    /**
     * 模拟生产环境的 JWT 认证：principal 是 userId claim（Long）
     */
    private void loginAsJwt(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private void loginAsOwnerOf(Integer characterId, Integer corpId) {
        loginAsJwt(CURRENT_USER_ID, "USER");
        EveAccount a = new EveAccount();
        a.setCharacterId(characterId);
        a.setCorpId(corpId);
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(a));
    }

    @Test
    @DisplayName("同步人物任务 -> 按 isCor=false / includeCompleted=false 调用")
    void syncJobs_character() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID, OWN_CORP_ID);

        jobApplicationService.syncJobs("char", OWN_CHARACTER_ID, "running");

        verify(industryJobService).batchInsertOrUpdateFromEsi(OWN_CHARACTER_ID, false, false);
    }

    @Test
    @DisplayName("同步军团已完成任务 -> 按 isCor=true / includeCompleted=true 调用")
    void syncJobs_corporationComplete() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID, OWN_CORP_ID);

        jobApplicationService.syncJobs("corporation", OWN_CORP_ID, "complete");

        verify(industryJobService).batchInsertOrUpdateFromEsi(OWN_CORP_ID, true, true);
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncJobs_parseError_throws() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID, OWN_CORP_ID);
        doThrow(new ParseException("bad", 0)).when(industryJobService)
                .batchInsertOrUpdateFromEsi(OWN_CHARACTER_ID, false, false);

        assertThrows(EveHelperException.class,
                () -> jobApplicationService.syncJobs("char", OWN_CHARACTER_ID, "running"));
    }

    @Test
    @DisplayName("同步他人角色 -> 拒绝，且不得触达 ESI 同步（IDOR 防御）")
    void syncOtherCharacter_throws() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID, OWN_CORP_ID);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> jobApplicationService.syncJobs("char", OTHER_CHARACTER_ID, "complete"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(industryJobService, never()).batchInsertOrUpdateFromEsi(anyInt(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("未认证访问 -> 拒绝，且不得触达 ESI 同步（fail-closed）")
    void unauthenticated_throws() throws Exception {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> jobApplicationService.syncJobs("char", OWN_CHARACTER_ID, "complete"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(industryJobService, never()).batchInsertOrUpdateFromEsi(anyInt(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("ROOT 角色同步他人 -> 放行，且不查归属")
    void rootSyncOther_allowed() throws Exception {
        loginAsJwt(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);

        jobApplicationService.syncJobs("char", OTHER_CHARACTER_ID, "complete");

        verify(industryJobService).batchInsertOrUpdateFromEsi(OTHER_CHARACTER_ID, true, false);
        verify(eveAccountService, never()).getAccountList(any());
    }
}
