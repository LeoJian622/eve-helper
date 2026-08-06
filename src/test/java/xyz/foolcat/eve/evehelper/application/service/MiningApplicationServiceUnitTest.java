package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.MiningDetailService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 月矿采掘应用服务单元测试。
 * <p>
 * 除编排逻辑外，重点验证归属校验（IDOR 防御）：该接口按用户可控的 characterId
 * 同步 ESI 数据，须先确认该人物属于当前用户。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("月矿采掘应用服务单元测试")
class MiningApplicationServiceUnitTest {

    private static final Integer CURRENT_USER_ID = 5;

    private static final Integer OWN_CHARACTER_ID = 2112818290;

    private static final Integer OTHER_CHARACTER_ID = 90000001;

    private static final Long OBSERVER_ID = 1030000000001L;

    @Mock
    MiningDetailService miningDetailService;

    @Mock
    EveAccountService eveAccountService;

    private MiningApplicationService miningApplicationService;

    @BeforeEach
    void setUp() {
        // 使用真实 AccessGuard，保证守卫逻辑本身被覆盖而非被 mock 掉
        AccessGuard accessGuard = new AccessGuard(new ResourceOwnershipPolicy(eveAccountService));
        miningApplicationService = new MiningApplicationService(miningDetailService, accessGuard);
    }

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    private void loginAsJwt(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private void loginAsOwnerOf(Integer characterId) {
        loginAsJwt(CURRENT_USER_ID, "USER");
        EveAccount a = new EveAccount();
        a.setCharacterId(characterId);
        a.setCorpId(98000001);
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(a));
    }

    @Test
    @DisplayName("同步成功 -> 调用 saveObserverMining")
    void syncMiningByObserver_success() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID);

        miningApplicationService.syncMiningByObserver(OWN_CHARACTER_ID, OBSERVER_ID);

        verify(miningDetailService).saveObserverMining(OWN_CHARACTER_ID, OBSERVER_ID);
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncMiningByObserver_parseError_throws() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID);
        doThrow(new ParseException("bad", 0)).when(miningDetailService)
                .saveObserverMining(OWN_CHARACTER_ID, OBSERVER_ID);

        assertThrows(EveHelperException.class,
                () -> miningApplicationService.syncMiningByObserver(OWN_CHARACTER_ID, OBSERVER_ID));
    }

    @Test
    @DisplayName("同步他人角色 -> 拒绝，且不得触达 ESI 同步（IDOR 防御）")
    void syncOtherCharacter_throws() throws Exception {
        loginAsOwnerOf(OWN_CHARACTER_ID);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> miningApplicationService.syncMiningByObserver(OTHER_CHARACTER_ID, OBSERVER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(miningDetailService, never()).saveObserverMining(anyInt(), anyLong());
    }

    @Test
    @DisplayName("未认证访问 -> 拒绝，且不得触达 ESI 同步（fail-closed）")
    void unauthenticated_throws() throws Exception {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> miningApplicationService.syncMiningByObserver(OWN_CHARACTER_ID, OBSERVER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(miningDetailService, never()).saveObserverMining(anyInt(), anyLong());
    }

    @Test
    @DisplayName("ROOT 角色同步他人 -> 放行，且不查归属")
    void rootSyncOther_allowed() throws Exception {
        loginAsJwt(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);

        miningApplicationService.syncMiningByObserver(OTHER_CHARACTER_ID, OBSERVER_ID);

        verify(miningDetailService).saveObserverMining(OTHER_CHARACTER_ID, OBSERVER_ID);
        verify(eveAccountService, never()).getAccountList(any());
    }
}
