package xyz.foolcat.eve.evehelper.domain.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 资源归属策略单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("资源归属策略单元测试")
class ResourceOwnershipPolicyUnitTest {

    private static final int USER_ID = 7;

    private static final String CHARACTER_ID = "2112832425";

    private static final String CORP_ID = "98000001";

    @Mock
    EveAccountService eveAccountService;

    private ResourceOwnershipPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ResourceOwnershipPolicy(eveAccountService);
        EveAccount account = new EveAccount();
        account.setCharacterId(Integer.valueOf(CHARACTER_ID));
        account.setCorpId(Integer.valueOf(CORP_ID));
        when(eveAccountService.getAccountList(USER_ID)).thenReturn(List.of(account));
    }

    @Test
    @DisplayName("本人角色ID -> 拥有")
    void ownsOwnCharacter() {
        assertTrue(policy.isOwnedBy(USER_ID, CHARACTER_ID));
    }

    @Test
    @DisplayName("本人角色所属军团ID -> 拥有")
    void ownsOwnCorporation() {
        assertTrue(policy.isOwnedBy(USER_ID, CORP_ID));
    }

    @Test
    @DisplayName("他人ID -> 不拥有")
    void doesNotOwnOthers() {
        assertFalse(policy.isOwnedBy(USER_ID, "9999"));
    }

    @Test
    @DisplayName("用户ID 非法（null 或非正）-> 不拥有")
    void rejectsInvalidUserId() {
        assertFalse(policy.isOwnedBy(null, CHARACTER_ID));
        assertFalse(policy.isOwnedBy(-1, CHARACTER_ID));
        assertFalse(policy.isOwnedBy(0, CHARACTER_ID));
    }

    @Test
    @DisplayName("所有者ID 为空 -> 不拥有")
    void rejectsBlankOwnerId() {
        assertFalse(policy.isOwnedBy(USER_ID, null));
        assertFalse(policy.isOwnedBy(USER_ID, "  "));
    }

    @Test
    @DisplayName("名下无角色 -> 不拥有")
    void rejectsWhenNoAccounts() {
        when(eveAccountService.getAccountList(USER_ID)).thenReturn(List.of());

        assertFalse(policy.isOwnedBy(USER_ID, CHARACTER_ID));
    }

    @Test
    @DisplayName("账户列表为 null -> 不拥有（防御）")
    void rejectsWhenAccountsNull() {
        when(eveAccountService.getAccountList(USER_ID)).thenReturn(null);

        assertFalse(policy.isOwnedBy(USER_ID, CHARACTER_ID));
    }

    @Test
    @DisplayName("军团ID 为 null 的账户不参与比对，不与字符串 \"null\" 误匹配")
    void nullCorpIdDoesNotMatch() {
        EveAccount noCorp = new EveAccount();
        noCorp.setCharacterId(Integer.valueOf(CHARACTER_ID));
        noCorp.setCorpId(null);
        when(eveAccountService.getAccountList(USER_ID)).thenReturn(List.of(noCorp));

        assertFalse(policy.isOwnedBy(USER_ID, "null"));
        assertTrue(policy.isOwnedBy(USER_ID, CHARACTER_ID));
    }

    @Test
    @DisplayName("前导零形式不匹配，避免同一数值的多种表示绕过比对")
    void leadingZeroDoesNotMatch() {
        assertFalse(policy.isOwnedBy(USER_ID, "000" + CHARACTER_ID));
    }

    @Test
    @DisplayName("多角色时任一命中即拥有")
    void anyAccountMatches() {
        EveAccount first = new EveAccount();
        first.setCharacterId(111);
        first.setCorpId(222);
        EveAccount second = new EveAccount();
        second.setCharacterId(Integer.valueOf(CHARACTER_ID));
        second.setCorpId(Integer.valueOf(CORP_ID));
        when(eveAccountService.getAccountList(USER_ID)).thenReturn(List.of(first, second));

        assertTrue(policy.isOwnedBy(USER_ID, CHARACTER_ID));
        assertTrue(policy.isOwnedBy(USER_ID, "222"));
    }
}
