package xyz.foolcat.eve.evehelper.domain.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;

/**
 * 游戏资源归属策略。
 * 判定某用户是否有权访问以人物或军团 ID 标识的资源，用于防御越权访问（IDOR）。
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceOwnershipPolicy {

    private final EveAccountService eveAccountService;

    /**
     * 判断用户是否拥有该人物或军团。
     * 允许本人名下任一角色的 characterId，或这些角色所属的 corpId
     * （军团成员可访问本团资源）。
     *
     * @param userId  用户ID
     * @param ownerId 人物或军团ID
     * @return 拥有则为 true
     */
    public boolean isOwnedBy(Integer userId, String ownerId) {
        if (userId == null || userId <= 0 || ownerId == null || ownerId.isBlank()) {
            return false;
        }
        List<EveAccount> accounts = eveAccountService.getAccountList(userId);
        if (accounts == null || accounts.isEmpty()) {
            return false;
        }
        return accounts.stream().anyMatch(account ->
                matches(ownerId, account.getCharacterId())
                        || matches(ownerId, account.getCorpId()));
    }

    /**
     * 严格按字符串比对，避免同一数值的多种表示（如前导零）绕过归一化差异
     */
    private boolean matches(String ownerId, Integer candidate) {
        return candidate != null && ownerId.equals(String.valueOf(candidate));
    }
}
