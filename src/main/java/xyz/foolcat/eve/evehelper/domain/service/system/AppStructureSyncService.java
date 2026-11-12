package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * APP 建筑同步领域服务（T030,US4）。
 * <p>
 * 遍历所有绑定角色的军团,逐军团选一个有建筑权限的角色,
 * 拉取 ESI 建筑列表并以该角色属主身份落库（014 语义：userId 归属同步者）。
 * <p>
 * 节流:每军团每小时最多同步一次(SETNX 3600s)。
 * 不复用 {@code StructureService.batchInsertOrUpdateFromEsi}(R13:其硬编码 SYSTEM_USER_ID)。
 *
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppStructureSyncService {

    private final EveAccountRepository eveAccountRepository;
    private final StructureRepository structureRepository;
    private final EsiGateway esiGateway;
    private final AuthorizeUtil authorizeUtil;
    private final CacheGateway cacheGateway;

    /** 节流键前缀 */
    private static final String THROTTLE_KEY_PREFIX = "app:struct:sync:";

    /** 节流 TTL(秒):1 小时 */
    private static final long THROTTLE_TTL_SECONDS = 3600L;

    /**
     * 同步所有军团建筑(入口方法,由定时任务调用)。
     * <p>
     * 逐军团隔离:任一军团失败不影响其他军团继续同步。
     */
    public void syncAllCorps() {
        List<EveAccount> all = eveAccountRepository.listAll();
        List<Integer> corpIds = all.stream()
                .map(EveAccount::getCorpId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (Integer corpId : corpIds) {
            try {
                syncOneCorp(corpId);
            } catch (Exception e) {
                log.error("建筑同步失败 corpId={}", corpId, e);
            }
        }
    }

    /**
     * 同步单个军团建筑。
     */
    private void syncOneCorp(Integer corpId) throws Exception {
        // ① 节流:SETNX 1 小时内不重复同步
        Boolean ok = cacheGateway.setIfAbsent(
                THROTTLE_KEY_PREFIX + corpId, "1", THROTTLE_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(ok)) {
            return;
        }

        // ② 选角:找第一个有建筑权限的角色
        List<EveAccount> candidates = eveAccountRepository.listByCorpId(corpId);
        EveAccount selectedAccount = null;
        String selectedToken = null;

        for (EveAccount candidate : candidates) {
            try {
                EveAccount account = authorizeUtil.authorizeInternal(candidate.getUserId(), candidate.getCharacterId());
                CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(
                        account.getCharacterId(), account.getUserId());
                String token = tokenResult.accessToken();
                CharacterRolesInfo roles = esiGateway.queryCharacterRoles(account.getCharacterId(), token);
                if (roles != null && roles.hasStructurePermission()) {
                    selectedAccount = account;
                    selectedToken = token;
                    break;
                }
            } catch (Exception e) {
                log.warn("建筑同步选角失败 corpId={}, characterId={}: {}",
                        corpId, candidate.getCharacterId(), e.getMessage());
            }
        }

        // ③ 无选中角色 → 跳过
        if (selectedAccount == null) {
            log.info("军团无建筑权限角色,跳过 corpId={}", corpId);
            return;
        }

        // ④ 拉取:分页 stream 镜像 StructureService 范式
        Integer maxPage = esiGateway.queryCorporationStructuresMaxPage(corpId, selectedToken);
        // IMP-A: ESI 失败(maxPage==null) → 跳过同步且不清理（防瞬时失败误删）
        if (maxPage == null) {
            log.info("军团建筑 ESI 拉取失败(maxPage=null),跳过且不清理 corpId={}", corpId);
            return;
        }
        if (maxPage <= 0) {
            log.info("军团建筑无分页数据,跳过 corpId={}", corpId);
            return;
        }

        final String token = selectedToken;
        List<Structure> structures = Stream.iterate(1, i -> i + 1)
                .limit(maxPage)
                .map(i -> esiGateway.queryCorporationStructures(corpId, "zh", i, token)
                        .collectList().block())
                .sequential()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // IMP-A: 归属:014 语义 userId 随同步者属主（仅设新行属主，upsert 变体不翻转既有行）
        Long ownerUserId = selectedAccount.getUserId().longValue();
        structures.forEach(s -> s.setUserId(ownerUserId));

        // IMP-A: 落库用归属保留变体（不翻转既有行 user_id）
        if (!structures.isEmpty()) {
            structureRepository.batchInsertOrUpdatePreserveOwner(structures);
        }

        // IMP-A: stale 清理（ESI 拉取成功后执行；structures 空→freshIds 空→删该 corp 全行）
        List<Long> freshIds = structures.stream()
                .map(Structure::getStructureId)
                .toList();
        structureRepository.removeStaleByCorporation(corpId, freshIds);

        log.info("建筑同步完成 corpId={}, characterId={}, 条数={}, stale清理keepIds={}",
                corpId, selectedAccount.getCharacterId(), structures.size(), freshIds.size());
    }
}
