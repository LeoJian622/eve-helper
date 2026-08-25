package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.CorporationBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.UserWalletBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.BalanceQueryStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * WalletBalanceApplicationService 单元测试(TDD, 018)。
 *
 * <p>纯 Mockito 单测,聚焦按用户聚合钱包余额用例契约:</p>
 * <ul>
 *     <li>空账户列表 → 返回空结构</li>
 *     <li>单人物无军团 → characters 1条,corporations 空</li>
 *     <li>多人物同军团 → corporations 按 corpId 去重</li>
 *     <li>多角色权限合并 → division 合并(union by division)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletBalanceApplicationService 按用户聚合余额用例")
class WalletBalanceApplicationServiceTest {

    private static final int USER_ID = 777;
    private static final int CID_1 = 9001;
    private static final int CID_2 = 9002;
    private static final int CID_3 = 9003;
    private static final int CORP_A = 98001;
    private static final int CORP_B = 98002;

    @Mock
    private EveAccountRepository eveAccountRepository;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EsiGateway esiGateway;

    @InjectMocks
    private WalletBalanceApplicationService applicationService;

    /* ============================ 辅助方法 ============================ */

    private EveAccount buildAccount(int characterId, String characterName, Integer corpId, String corpName) {
        EveAccount account = new EveAccount();
        account.setCharacterId(characterId);
        account.setCharacterName(characterName);
        account.setCorpId(corpId);
        account.setCorpName(corpName);
        account.setUserId(USER_ID);
        return account;
    }

    private void stubAuthorize(int cid) {
        EveAccount account = buildAccount(cid, "Char-" + cid, null, null);
        when(authorizeUtil.authorize(cid)).thenReturn(account);
    }

    private void stubAccessToken(int cid) throws ParseException {
        lenient().when(esiGateway.getAccessToken(eq(cid), anyInt())).thenReturn("Bearer token-" + cid);
    }

    /**
     * 018 IMPORTANT-4:服务改为自行从安全上下文取身份,测试须注入认证主体。
     *
     * <p>principal 用 Integer(JWT 认证时的主体形态),UserUtil 按 Number 分支识别。</p>
     */
    private void authenticateAs(int userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
    }

    /** 默认以 USER_ID 已认证身份运行;需要未认证/异常主体的用例自行覆盖。 */
    @BeforeEach
    void authenticateByDefault() {
        authenticateAs(USER_ID);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /* ============================ 空账户列表 ============================ */

    @Nested
    @DisplayName("空账户列表场景")
    class EmptyAccountsTest {

        @Test
        @DisplayName("用户无绑定人物 → 返回空结构")
        void noAccounts_returnsEmptyStructure() {
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(Collections.emptyList());

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).isEmpty();
            assertThat(result.corporations()).isEmpty();
            verify(eveAccountRepository).getAccountList(USER_ID);
        }

        @Test
        @DisplayName("用户账户列表为 null → 返回空结构")
        void nullAccounts_returnsEmptyStructure() {
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(null);

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).isEmpty();
            assertThat(result.corporations()).isEmpty();
        }
    }

    /* ============================ 单人物无军团 ============================ */

    @Nested
    @DisplayName("单人物无军团场景")
    class SingleCharacterNoCorpTest {

        @Test
        @DisplayName("单人物无军团 → characters 1条, corporations 空")
        void singleCharacterNoCorp_returnsOneCharacterEmptyCorporations() throws ParseException {
            EveAccount account = buildAccount(CID_1, "Alice", null, null);
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1000000.0);

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).hasSize(1);
            CharacterBalanceVO charBalance = result.characters().get(0);
            assertThat(charBalance.characterId()).isEqualTo(CID_1);
            assertThat(charBalance.characterName()).isEqualTo("Alice");
            assertThat(charBalance.balance()).isEqualTo(1000000.0);
            assertThat(result.corporations()).isEmpty();
        }
    }

    /* ============================ 多人物同军团 ============================ */

    @Nested
    @DisplayName("多人物同军团场景")
    class MultipleCharactersSameCorpTest {

        @Test
        @DisplayName("3个人物属于同一军团 → corporations 只有1条(去重)")
        void threeCharactersSameCorp_corporationsDedup() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_A, "CorpA");
            EveAccount char3 = buildAccount(CID_3, "Charlie", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2, char3));

            // stub 人物余额
            for (int cid : new int[]{CID_1, CID_2, CID_3}) {
                stubAuthorize(cid);
                stubAccessToken(cid);
                when(esiGateway.queryCharacterWalletBalance(eq(cid), any())).thenReturn(100000.0);
            }

            // stub 军团余额（3个角色都返回相同的 division，使用 lenient 避免 unnecessary stubbing）
            lenient().when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(1, 500000.0),
                            new WalletDivisionBalance(2, 600000.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).hasSize(3);
            assertThat(result.corporations()).hasSize(1);
            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.corpId()).isEqualTo(CORP_A);
            assertThat(corpBalance.corpName()).isEqualTo("CorpA");
            // 总余额 = 500000 + 600000 = 1100000
            assertThat(corpBalance.totalBalance()).isEqualTo(1100000.0);
        }
    }

    /* ============================ 多角色权限合并 ============================ */

    @Nested
    @DisplayName("多角色权限合并场景")
    class MultiRolePermissionMergeTest {

        @Test
        @DisplayName("角色A看division 1-3, 角色B看division 4-7 → 合并后1-7都有")
        void disjointPermissions_mergedToFullDivisions() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2));

            stubAuthorize(CID_1);
            stubAuthorize(CID_2);
            stubAccessToken(CID_1);
            stubAccessToken(CID_2);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(100000.0);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_2), any())).thenReturn(200000.0);

            // 角色A只能看division 1-3
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(1, 100.0),
                            new WalletDivisionBalance(2, 200.0),
                            new WalletDivisionBalance(3, 300.0)))
                    // 角色B只能看division 4-7
                    .thenReturn(List.of(
                            new WalletDivisionBalance(4, 400.0),
                            new WalletDivisionBalance(5, 500.0),
                            new WalletDivisionBalance(6, 600.0),
                            new WalletDivisionBalance(7, 700.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.divisions()).hasSize(7);
            // 总余额 = 100+200+300+400+500+600+700 = 2800
            assertThat(corpBalance.totalBalance()).isEqualTo(2800.0);
        }

        @Test
        @DisplayName("角色A失败不影响角色B → 至少返回B的数据")
        void oneRoleFails_otherRoleStillReturns() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2));

            stubAuthorize(CID_1);
            stubAuthorize(CID_2);
            stubAccessToken(CID_1);
            stubAccessToken(CID_2);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(100000.0);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_2), any())).thenReturn(200000.0);

            // 角色A调用失败
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenThrow(new RuntimeException("ESI error"))
                    // 角色B成功
                    .thenReturn(List.of(new WalletDivisionBalance(1, 500.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            // 至少返回角色B的数据
            assertThat(corpBalance.divisions()).isNotEmpty();
            assertThat(corpBalance.totalBalance()).isEqualTo(500.0);
            // 018 IMPORTANT-2:角色A失败使分账集合不完整,须标记 partial 并列出失败人物,
            // 否则 totalBalance=500 与"军团账上真的只有 500"无法区分
            assertThat(corpBalance.partial()).isTrue();
            assertThat(corpBalance.unavailableCharacterIds()).containsExactly(CID_1);
        }

        @Test
        @DisplayName("全部分账均取到 → partial 为 false,失败列表为空")
        void allDivisionsResolved_notPartial() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1));

            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1.0);
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 100.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.partial()).isFalse();
            assertThat(corpBalance.unavailableCharacterIds()).isEmpty();
        }

        @Test
        @DisplayName("首个人物已覆盖 division 1..7 → 后续人物不再调用 ESI(018 IMPORTANT-5 提前终止)")
        void firstCharacterCoversAllDivisions_laterCharactersSkipped() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2));

            stubAuthorize(CID_1);
            stubAuthorize(CID_2);
            stubAccessToken(CID_1);
            stubAccessToken(CID_2);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1.0);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_2), any())).thenReturn(2.0);

            // 首个人物一次返回全部 7 个分账,后续人物的军团调用对结果零贡献
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(1, 10.0),
                            new WalletDivisionBalance(2, 20.0),
                            new WalletDivisionBalance(3, 30.0),
                            new WalletDivisionBalance(4, 40.0),
                            new WalletDivisionBalance(5, 50.0),
                            new WalletDivisionBalance(6, 60.0),
                            new WalletDivisionBalance(7, 70.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.divisions()).hasSize(7);
            assertThat(corpBalance.totalBalance()).isEqualTo(280.0);
            // 军团分账查询只发生一次:第二个人物被提前终止跳过,省下一次 ESI 配额
            verify(esiGateway, times(1)).queryCorporationWalletBalances(eq(CORP_A), any());
            // 且未标记 partial —— 跳过不等于失败
            assertThat(corpBalance.partial()).isFalse();
        }

        @Test
        @DisplayName("权限重叠 → 每个division取第一个值(去重)")
        void overlappingPermissions_dedupByDivision() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2));

            stubAuthorize(CID_1);
            stubAuthorize(CID_2);
            stubAccessToken(CID_1);
            stubAccessToken(CID_2);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(100000.0);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_2), any())).thenReturn(200000.0);

            // 两个角色都返回 division 1,但余额不同
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 100.0)))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 999.0))); // 应被忽略

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            // division 1 只出现一次,取第一个值
            assertThat(corpBalance.divisions()).hasSize(1);
            assertThat(corpBalance.divisions().get(0).balance()).isEqualTo(100.0);
            assertThat(corpBalance.totalBalance()).isEqualTo(100.0);
        }
        @Test
        @DisplayName("ESI 返回 division 为 null → 跳过该条,不抛 NPE 且不影响其他分账(018 IMPORTANT-3)")
        void nullDivision_skippedWithoutNpe() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1));

            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(100000.0);

            // 畸形 ESI 响应:混入一条 division 为 null 的记录。
            // 原实现把 null 存进 HashMap 的 key,随后 Comparator.comparingInt 拆箱 → NPE,
            // 且该 NPE 抛在单角色 try/catch 之外 → 整个端点 500。
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(null, 999.0),
                            new WalletDivisionBalance(1, 100.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            // 非法记录被跳过,合法分账照常返回
            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.divisions()).hasSize(1);
            assertThat(corpBalance.divisions().get(0).division()).isEqualTo(1);
            assertThat(corpBalance.divisions().get(0).balance()).isEqualTo(100.0);
            // 非法记录的 999 不计入总额
            assertThat(corpBalance.totalBalance()).isEqualTo(100.0);
            // 人物余额不受军团侧畸形数据影响
            assertThat(result.characters().get(0).balance()).isEqualTo(100000.0);
        }

        @Test
        @DisplayName("ESI 返回 division 越界(0/8) → 跳过该条,只保留 1..7 合法分账(018 IMPORTANT-3)")
        void outOfRangeDivision_skipped() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1));

            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(0.0);

            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(0, 111.0),
                            new WalletDivisionBalance(8, 222.0),
                            new WalletDivisionBalance(7, 700.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CorporationBalanceVO corpBalance = result.corporations().get(0);
            assertThat(corpBalance.divisions()).hasSize(1);
            assertThat(corpBalance.divisions().get(0).division()).isEqualTo(7);
            assertThat(corpBalance.totalBalance()).isEqualTo(700.0);
        }
    }

    /* ============================ 多人物不同军团 ============================ */

    @Nested
    @DisplayName("多人物不同军团场景")
    class MultipleCharactersDifferentCorpTest {

        @Test
        @DisplayName("2个人物分属2个军团 → corporations 有2条")
        void twoCharactersTwoCorps_corporationsTwoEntries() throws ParseException {
            EveAccount char1 = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            EveAccount char2 = buildAccount(CID_2, "Bob", CORP_B, "CorpB");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(char1, char2));

            stubAuthorize(CID_1);
            stubAuthorize(CID_2);
            stubAccessToken(CID_1);
            stubAccessToken(CID_2);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(100000.0);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_2), any())).thenReturn(200000.0);

            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 500.0)));
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_B), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 600.0)));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).hasSize(2);
            assertThat(result.corporations()).hasSize(2);
            assertThat(result.corporations())
                    .extracting(CorporationBalanceVO::corpId)
                    .containsExactlyInAnyOrder(CORP_A, CORP_B);
        }
    }

    /* ============================ 边界场景 ============================ */

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTest {

        @Test
        @DisplayName("未认证(安全上下文为空)→ 抛 ACCESS_UNAUTHORIZED,不触达仓储(018 IMPORTANT-4)")
        void unauthenticated_throwsUnauthorized() {
            // 清掉默认注入的身份:UserUtil.getUserId() 返回 -1
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> applicationService.getUserWalletBalance())
                    .isInstanceOf(EveHelperException.class);
            // 拒绝发生在取数之前,不泄漏任何账号列表
            verifyNoInteractions(eveAccountRepository);
        }

        @Test
        @DisplayName("主体 userId ≤ 0 → 抛 ACCESS_UNAUTHORIZED,不触达仓储(018 IMPORTANT-4)")
        void nonPositiveUserId_throwsUnauthorized() {
            for (int bad : new int[]{0, -1}) {
                authenticateAs(bad);
                assertThatThrownBy(() -> applicationService.getUserWalletBalance())
                        .isInstanceOf(EveHelperException.class);
            }
            verifyNoInteractions(eveAccountRepository);
        }

        @Test
        @DisplayName("人物余额上游故障 → balance 为 null + UPSTREAM_ERROR(018 IMPORTANT-2)")
        void characterBalanceUpstreamError_nullWithStatus() throws ParseException {
            authenticateAs(USER_ID);
            EveAccount account = buildAccount(CID_1, "Alice", null, null);
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any()))
                    .thenThrow(new RuntimeException("ESI error"));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            assertThat(result.characters()).hasSize(1);
            CharacterBalanceVO cb = result.characters().get(0);
            // 关键:不再降级为 0.0,否则与真实的零余额同形
            assertThat(cb.balance()).isNull();
            assertThat(cb.status()).isEqualTo(BalanceQueryStatus.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("人物授权失效(归属校验抛 AUT00208) → balance 为 null + UNAUTHORIZED(018 IMPORTANT-2)")
        void characterUnauthorized_nullWithStatus() {
            authenticateAs(USER_ID);
            EveAccount account = buildAccount(CID_1, "Alice", null, null);
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            // 归属校验失败(账号已解绑/不属于当前用户)
            when(authorizeUtil.authorize(CID_1))
                    .thenThrow(new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CharacterBalanceVO cb = result.characters().get(0);
            assertThat(cb.balance()).isNull();
            // 授权类失败须与上游故障区分,前者用户可自行重新授权
            assertThat(cb.status()).isEqualTo(BalanceQueryStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("ESI 返回 null 余额 → 视为可信的 0 而非失败(status 仍为 OK)")
        void esiReturnsNullBalance_treatedAsTrustedZero() throws ParseException {
            authenticateAs(USER_ID);
            EveAccount account = buildAccount(CID_1, "Alice", null, null);
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(null);

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CharacterBalanceVO cb = result.characters().get(0);
            assertThat(cb.balance()).isEqualTo(0.0);
            assertThat(cb.status()).isEqualTo(BalanceQueryStatus.OK);
        }

        @Test
        @DisplayName("同一人物在单次请求内只 authorize 一次(018 IMPORTANT-5 去重)")
        void sameCharacterAuthorizedOnlyOncePerRequest() throws ParseException {
            authenticateAs(USER_ID);
            // 有军团的人物会同时走人物余额路径与军团余额路径,原实现会 authorize 两次
            EveAccount account = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1.0);
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 2.0)));

            applicationService.getUserWalletBalance();

            verify(authorizeUtil, times(1)).authorize(CID_1);
        }

        @Test
        @DisplayName("token 换取锚定 (cid, 当前 userId),且军团查询用该人物的 token(安全审计 MEDIUM-3)")
        void tokenExchangeAnchoredToOwnerUserId() throws ParseException {
            authenticateAs(USER_ID);
            EveAccount account = buildAccount(CID_1, "Alice", CORP_A, "CorpA");
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1.0);
            when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(1, 2.0)));

            applicationService.getUserWalletBalance();

            // userId 实参正是本仓库历史 ESI token 缓存键越权漏洞(f153ed4)所在参数,
            // 缓存键为 esi_access_token:<userId>:<characterId>。若有人把它换成常量、
            // SYSTEM_USER_ID 或另一个 account 的 userId(017 IMPORTANT-1 的真实失败模式),
            // 以下断言必须失败。
            //
            // times(2) 而非 times(1):authorize 已按 cid 去重,但 getAccessToken 仍在
            // 人物余额与军团余额两条路径各调一次。这钉死了一个事实 —— authorize 去重的
            // 实际收益小于表面:getAccessToken 内部(EsiApiService.doGetAccessToken)
            // 在读 Redis 前还会做一次 getAccountOne 库查,故 DB 查询只从 4N 降到 3N,
            // 而非 2N→N(2026-08-25 安全审计 HIGH-1 第 2 点)。token 级去重待独立立项。
            verify(esiGateway, times(2)).getAccessToken(CID_1, USER_ID);
            // 军团分账必须用该人物换来的 token,而非任意 token
            verify(esiGateway).queryCorporationWalletBalances(CORP_A, "Bearer token-" + CID_1);
        }

        @Test
        @DisplayName("ESI 403 权限不足 → PERMISSION_DENIED 而非 UPSTREAM_ERROR(安全审计 MEDIUM-1)")
        void esiPermissionLow_mappedToPermissionDenied() throws ParseException {
            authenticateAs(USER_ID);
            EveAccount account = buildAccount(CID_1, "Alice", null, null);
            when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));
            stubAuthorize(CID_1);
            stubAccessToken(CID_1);
            // 模拟 ESI 403:token 有效但该人物缺少目标军团角色(码 ESI00403)
            when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any()))
                    .thenThrow(new EveHelperException(new StubEsiPermissionLowCode()));

            UserWalletBalanceVO result = applicationService.getUserWalletBalance();

            CharacterBalanceVO cb = result.characters().get(0);
            assertThat(cb.balance()).isNull();
            // 关键:不得归为 UPSTREAM_ERROR —— 那的语义是"重试可能恢复",
            // 会诱使前端反复重试持久性权限缺失,每次撞 ESI 403 并消耗全应用 error limit
            assertThat(cb.status()).isEqualTo(BalanceQueryStatus.PERMISSION_DENIED);
        }
    }

    /**
     * 桩：模拟 infrastructure 层 ResultCode.ESI_AUTH_PERMISSION_LOW("ESI00403")。
     *
     * <p>测试位于 application 层，不得 import infrastructure 类型（DDD 分层规则），
     * 故以实现 {@link xyz.foolcat.eve.evehelper.shared.result.IResultCode} 的桩复现该码值。
     * 这同时把「码值 ESI00403」这一跨层约定钉死在测试里 —— 若 infrastructure 侧改了码值
     * 而忘记同步服务内的常量，本用例仍绿但生产会退化，故另见 knowledge 文档中的同步提醒。</p>
     */
    private static final class StubEsiPermissionLowCode
            implements xyz.foolcat.eve.evehelper.shared.result.IResultCode {
        @Override
        public String getCode() {
            return "ESI00403";
        }

        @Override
        public String getMsg() {
            return "该人物缺少目标军团的 Director/Bookkeeper 等角色";
        }
    }
}
