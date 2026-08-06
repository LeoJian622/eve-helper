package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 蓝图应用服务单元测试。
 * 重点验证查询条件被完整传递到领域仓储，而非静默丢弃；以及归属校验防御 IDOR。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("蓝图应用服务单元测试")
class BlueprintsApplicationServiceUnitTest {

    /**
     * 当前登录用户ID
     */
    private static final int CURRENT_USER_ID = 7;

    /**
     * 该用户名下的角色ID
     */
    private static final String OWNED_CHARACTER_ID = "2112832425";

    @Mock
    BlueprintsRepository blueprintsRepository;

    @Mock
    BlueprintsAssembler blueprintsAssembler;

    @Mock
    EveAccountService eveAccountService;

    private BlueprintsApplicationService service;

    @BeforeEach
    void setUp() {
        service = new BlueprintsApplicationService(
                blueprintsRepository, blueprintsAssembler,
                new AccessGuard(new ResourceOwnershipPolicy(eveAccountService)));
        loginAs(CURRENT_USER_ID, "USER");
        // 默认：当前用户名下持有 OWNED_CHARACTER_ID 这个角色
        EveAccount owned = new EveAccount();
        owned.setCharacterId(Integer.valueOf(OWNED_CHARACTER_ID));
        owned.setCorpId(98000001);
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(owned));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 模拟指定用户与角色登录（principal 为 SysUser，供部分内部调用路径使用）
     */
    private void loginAs(Integer userId, String role) {
        SysUser user = new SysUser();
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    /**
     * 模拟生产环境的 JWT 认证：principal 是 userId claim（Long），
     * 与 JwtAuthorizationTokenFilter 放入安全上下文的类型一致
     */
    private void loginAsJwt(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private void stubRepositoryReturningEmptyPage() {
        when(blueprintsRepository.selectBlueprintsInvtypeUniverse(any(BlueprintsPageCriteria.class)))
                .thenReturn(PageResult.<BlueprintsDTO>builder()
                        .records(List.of())
                        .total(0L)
                        .current(1L)
                        .size(20L)
                        .pages(0L)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());
        when(blueprintsAssembler.dto2Vo(List.of())).thenReturn(List.of());
    }

    private BlueprintsPageCriteria captureCriteria() {
        ArgumentCaptor<BlueprintsPageCriteria> captor = ArgumentCaptor.forClass(BlueprintsPageCriteria.class);
        verify(blueprintsRepository).selectBlueprintsInvtypeUniverse(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("蓝图名称筛选条件传递到仓储，不被静默丢弃")
    void passesBlueprintNameToRepository() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .blueprintName("恶狼级")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals("恶狼级", captureCriteria().getBlueprintName());
    }

    @Test
    @DisplayName("空白蓝图名称归一为 null，避免生成无意义的 like 条件")
    void blankBlueprintNameNormalizedToNull() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .blueprintName("   ")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertNull(captureCriteria().getBlueprintName());
    }

    @Test
    @DisplayName("blueprintType=copy 映射为仅拷贝筛选")
    void mapsCopyTypeFilter() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .blueprintType("copy")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.COPY, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("blueprintType=original 映射为仅原图筛选，且大小写不敏感")
    void mapsOriginalTypeFilterIgnoringCase() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .blueprintType("ORIGINAL")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.ORIGINAL, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("blueprintType 缺省时不筛选原图/拷贝")
    void defaultsToAnyCopyFilter() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId(OWNED_CHARACTER_ID).build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.ANY, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("非法 blueprintType 抛出业务异常，而非静默忽略")
    void rejectsUnknownBlueprintType() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .blueprintType("bpo-or-something")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("白名单内的排序字段被解析并传递")
    void mapsWhitelistedSortField() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .sortField("typeName")
                .sortOrder("asc")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        BlueprintsPageCriteria criteria = captureCriteria();
        assertEquals(BlueprintsPageCriteria.SortField.TYPE_NAME, criteria.getSortField());
        assertTrue(criteria.isAscending());
    }

    @Test
    @DisplayName("白名单外的排序字段被拒绝，阻断 SQL 注入入口")
    void rejectsSortFieldOutsideWhitelist() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .sortField("bt.item_id; drop table blueprints--")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("非法排序方向被拒绝，而非静默降级为降序")
    void rejectsUnknownSortOrder() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .sortField("runs")
                .sortOrder("ascending")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("所有者ID 必须为数字，避免下推数据库做隐式转换")
    void rejectsNonNumericOwnerId() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("abc").build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("分页参数传递到仓储条件")
    void passesPaginationToRepository() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId(OWNED_CHARACTER_ID)
                .current(3)
                .size(50)
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        BlueprintsPageCriteria criteria = captureCriteria();
        assertEquals(3L, criteria.getCurrent());
        assertEquals(50L, criteria.getSize());
    }

    @Test
    @DisplayName("所有者ID 为空时抛出业务异常")
    void rejectsBlankOwnerId() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("  ").build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("查询他人角色的蓝图被拒绝（IDOR 防御）")
    void rejectsOtherUsersCharacter() {
        // Arrange：9999 不在当前用户名下
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("9999").build();

        // Act & Assert：必须是越权错误码，而非参数校验错误
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("查询本人所属军团的蓝图被允许")
    void allowsOwnCorporation() {
        // Arrange：98000001 是当前用户角色所属军团
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("98000001").build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals("98000001", captureCriteria().getOwnerId());
    }

    @Test
    @DisplayName("ROOT 角色可查询任意所有者的蓝图")
    void rootBypassesOwnershipCheck() {
        // Arrange
        loginAs(CURRENT_USER_ID, GlobalConstants.ROOT_ROLE_CODE);
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("9999").build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals("9999", captureCriteria().getOwnerId());
    }

    @Test
    @DisplayName("用户名下无任何角色时，查询任何所有者都被拒绝")
    void rejectsWhenUserOwnsNothing() {
        // Arrange
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of());
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId(OWNED_CHARACTER_ID).build();

        // Act & Assert
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("未认证访问被拒绝，不退化为放行")
    void rejectsUnauthenticated() {
        // Arrange：清空安全上下文模拟未认证
        SecurityContextHolder.clearContext();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId(OWNED_CHARACTER_ID).build();

        // Act & Assert
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("归属校验先于参数解析，越权请求不泄露参数校验细节")
    void ownershipCheckedBeforeParamParsing() {
        // Arrange：他人 ID + 非法排序字段
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("9999")
                .sortField("不存在的字段")
                .build();

        // Act
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));

        // Assert：应报越权而非排序字段错误
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("JWT 认证下（principal 为 Long）归属校验同样生效 —— 覆盖生产实际路径")
    void ownershipCheckWorksWithJwtPrincipal() {
        // Arrange：模拟 JwtAuthorizationTokenFilter 放入的 Long principal
        loginAsJwt(CURRENT_USER_ID, "USER");
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("9999").build();

        // Act & Assert
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("JWT 认证下查询本人角色被允许")
    void allowsOwnCharacterWithJwtPrincipal() {
        // Arrange
        loginAsJwt(CURRENT_USER_ID, "USER");
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId(OWNED_CHARACTER_ID).build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(OWNED_CHARACTER_ID, captureCriteria().getOwnerId());
    }

    @Test
    @DisplayName("匿名 principal（非 SysUser/非 Long）被拒绝，不因类型异常而放行或 500")
    void rejectsAnonymousPrincipal() {
        // Arrange：Spring Security 匿名访问时 principal 是字符串
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId(OWNED_CHARACTER_ID).build();

        // Act & Assert
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> service.queryBlueprintsByPage(query));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("前导零形式的所有者ID被拒绝，避免同一行的多种表示绕过归属比对")
    void rejectsLeadingZeroOwnerId() {
        // Arrange：数值上等于本人角色，但字符串表示不同
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("000" + OWNED_CHARACTER_ID)
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("超长数字所有者ID被拒绝，避免下推数据库隐式转换")
    void rejectsOverlongOwnerId() {
        // Arrange：超出 BIGINT 范围
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("99999999999999999999")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("军团ID 为空的账户不参与归属比对，不产生意外匹配")
    void nullCorpIdDoesNotMatch() {
        // Arrange
        EveAccount noCorp = new EveAccount();
        noCorp.setCharacterId(Integer.valueOf(OWNED_CHARACTER_ID));
        noCorp.setCorpId(null);
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of(noCorp));
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("98000001").build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }
}
