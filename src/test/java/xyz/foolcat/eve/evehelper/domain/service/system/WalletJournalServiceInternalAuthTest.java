package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletJournalService.syncCorporationJournal 内部路径归属对账契约测试(FR-007)。
 *
 * <p>验证 013 的 M2 语义:定时任务等<b>无安全上下文</b>路径走
 * {@code authorizeInternal(SYSTEM_USER_ID, characterId) },其 owner 过滤由 eve_account
 * 双键查询 <code>WHERE user_id=? AND character_id=?</code> 硬性保证 —— owner 判定<b>不因系统身份旁路</b>:</p>
 * <ul>
 *     <li>库中无对应系统绑定行 → getAccountOne 抛 USER_ACCOUNT_NOT_EXIST → 内部同步被拒绝、不触发 ESI
 *         (测试 A,锁 fail-closed)。</li>
 *     <li>存在绑定行 → 仍走角色行 getCorpId() 派生军团ID,ESI 查询用解析值 (测试 B,真实拥有者定位不旁路)。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournalService.syncCorporationJournal 内部路径归属对账(FR-007)")
class WalletJournalServiceInternalAuthTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @InjectMocks
    private WalletJournalService walletJournalService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private EveAccount account(Integer userId, Integer corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCorpId(corpId);
        return a;
    }

    @Test
    @DisplayName("内部路径无系统绑定行:authorizeInternal(SYSTEM_USER_ID, characterId) fail-closed 拒绝,不触发 ESI")
    void internalPathPinsSystemIdentityAndFailClosed() {
        Integer characterId = 9001;
        // 无 SecurityContext → UserUtil.getUserId()=-1 → 内部路径 authorizeInternal(SYSTEM_USER_ID, characterId)
        // 库中无该系统绑定行 → getAccountOne 抛 USER_ACCOUNT_NOT_EXIST → fail-closed 拒绝
        when(authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, characterId))
                .thenThrow(new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST));

        assertThatThrownBy(() -> walletJournalService.syncCorporationJournal(characterId))
                .isInstanceOf(EveHelperException.class)
                .satisfies(e -> assertThat(((EveHelperException) e).resultCode)
                        .isEqualTo(ResultCode.USER_ACCOUNT_NOT_EXIST));

        // 归属校验失败 → 不触发任何 ESI 军团读取(拒绝而非静默以他人身份执行)
        verify(esiApiService, never()).queryCorporationWalletJournal(anyInt(), anyInt(), anyInt(), any());
        verify(esiApiService, never()).queryCorporationWalletJournalMaxPage(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("内部路径存在绑定行:仍从角色行 getCorpId() 派生军团ID,ESI 查询用解析值(真实拥有者定位不旁路)")
    void internalPathResolvesCorpId_usesParsedValue() throws Exception {
        Integer characterId = 9001;
        Integer validCorpId = 5001;
        String token = "Bearer internal-token";
        when(authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, characterId))
                .thenReturn(account(GlobalConstants.SYSTEM_USER_ID, validCorpId));
        when(esiApiService.getAccessToken(characterId, GlobalConstants.SYSTEM_USER_ID)).thenReturn(token);
        // division 1:maxPage=1 + 空页(无流水)。
        when(esiApiService.queryCorporationWalletJournalMaxPage(validCorpId, 1, token)).thenReturn(1);
        when(esiApiService.queryCorporationWalletJournal(validCorpId, 1, 1, token)).thenReturn(Flux.empty());
        // division 2..7:maxPage 未 mock → null → 0 页空,均成功,无失败隔离。

        walletJournalService.syncCorporationJournal(characterId);

        // 内部路径也以角色行派生 corpId 驱动 ESI(owner 定位不旁路);7 个 division 均用解析 corpId
        verify(esiApiService, org.mockito.Mockito.times(7))
                .queryCorporationWalletJournalMaxPage(eq(validCorpId), anyInt(), any());
    }
}