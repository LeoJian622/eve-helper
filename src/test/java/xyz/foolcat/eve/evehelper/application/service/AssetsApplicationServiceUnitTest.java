package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.application.assembler.system.AssetsAssembler;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资产应用服务单元测试。
 * 覆盖正常查询/同步用例，以及归属校验防御越权访问（IDOR）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("资产应用服务单元测试")
class AssetsApplicationServiceUnitTest {

    /**
     * 当前登录用户ID
     */
    private static final int CURRENT_USER_ID = 7;

    /**
     * 该用户名下的角色ID
     */
    private static final String OWNED_CHARACTER_ID = "2112832425";

    /**
     * 该用户角色所属军团ID
     */
    private static final String OWNED_CORP_ID = "98000001";

    @Mock
    AssetsService assetsService;

    @Mock
    AssetsAssembler assetsAssembler;

    @Mock
    EveAccountService eveAccountService;

    private AssetsApplicationService assetsApplicationService;

    @BeforeEach
    void setUp() {
        assetsApplicationService = new AssetsApplicationService(
                assetsService, assetsAssembler,
                new AccessGuard(new ResourceOwnershipPolicy(eveAccountService)));
        loginAs(CURRENT_USER_ID, "USER");
        EveAccount owned = new EveAccount();
        owned.setCharacterId(Integer.valueOf(OWNED_CHARACTER_ID));
        owned.setCorpId(Integer.valueOf(OWNED_CORP_ID));
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(owned));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 模拟生产环境的 JWT 认证：principal 为 userId claim
     */
    private void loginAs(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("同步本人角色 -> 调用 saveAndUpdateAsserts")
    void syncAssets_success() throws Exception {
        assetsApplicationService.syncAssets(Integer.valueOf(OWNED_CHARACTER_ID));

        verify(assetsService).saveAndUpdateAsserts(Integer.valueOf(OWNED_CHARACTER_ID));
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncAssets_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(assetsService)
                .saveAndUpdateAsserts(Integer.valueOf(OWNED_CHARACTER_ID));

        assertThrows(EveHelperException.class,
                () -> assetsApplicationService.syncAssets(Integer.valueOf(OWNED_CHARACTER_ID)));
    }

    @Test
    @DisplayName("查询本人资产清单 -> 返回转 VO 后的分页结果")
    void queryAssetsList_returnsVoPage() {
        Assets asset = new Assets();
        when(assetsService.getAssertsListById(OWNED_CHARACTER_ID, 0, 30)).thenReturn(List.of(asset));
        AssetsVO vo = new AssetsVO();
        when(assetsAssembler.domain2Vo(List.of(asset))).thenReturn(List.of(vo));

        PageResult<AssetsVO> result = assetsApplicationService.queryAssetsList(OWNED_CHARACTER_ID, 0, 30);

        assertTrue(result.getRecords().stream().anyMatch(v -> v == vo));
    }

    @Test
    @DisplayName("查询他人资产被拒绝（IDOR 防御）")
    void queryAssetsList_rejectsOtherOwner() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> assetsApplicationService.queryAssetsList("9999", 0, 30));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(assetsService, never()).getAssertsListById(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("查询本人所属军团资产被允许")
    void queryAssetsList_allowsOwnCorporation() {
        when(assetsService.getAssertsListById(OWNED_CORP_ID, 0, 30)).thenReturn(List.of());
        when(assetsAssembler.domain2Vo(List.of())).thenReturn(List.of());

        assetsApplicationService.queryAssetsList(OWNED_CORP_ID, 0, 30);

        verify(assetsService).getAssertsListById(OWNED_CORP_ID, 0, 30);
    }

    @Test
    @DisplayName("ROOT 角色可查询任意所有者资产")
    void queryAssetsList_rootBypasses() {
        loginAs(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);
        when(assetsService.getAssertsListById("9999", 0, 30)).thenReturn(List.of());
        when(assetsAssembler.domain2Vo(List.of())).thenReturn(List.of());

        assetsApplicationService.queryAssetsList("9999", 0, 30);

        verify(assetsService).getAssertsListById("9999", 0, 30);
    }

    @Test
    @DisplayName("未认证查询资产被拒绝")
    void queryAssetsList_rejectsUnauthenticated() {
        SecurityContextHolder.clearContext();

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> assetsApplicationService.queryAssetsList(OWNED_CHARACTER_ID, 0, 30));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(assetsService, never()).getAssertsListById(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("同步他人角色资产被拒绝，不触发 ESI 调用（越权写防御）")
    void syncAssets_rejectsOtherOwner() throws Exception {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> assetsApplicationService.syncAssets(9999));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(assetsService, never()).saveAndUpdateAsserts(anyInt());
    }

    @Test
    @DisplayName("未认证同步被拒绝，不触发 ESI 调用")
    void syncAssets_rejectsUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        assertThrows(EveHelperException.class,
                () -> assetsApplicationService.syncAssets(Integer.valueOf(OWNED_CHARACTER_ID)));

        verify(assetsService, never()).saveAndUpdateAsserts(anyInt());
    }
}
