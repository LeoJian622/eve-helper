package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.AppNotificationEventType;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * APP 通知检测领域服务。
 * <p>
 * 负责检测建筑燃料不足、工业完成、技能队列空等事件，并通过权限扇出推送给有权用户。
 * <p>
 * 本类仅实现 detectFuelLow() 及其共用私有 helper（T032 将追加工业/技能/清理方法）。
 *
 * @author Leojian
 * date 2026-11-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppNotificationDetectService {

    private final AppNotificationRepository appNotificationRepository;
    private final AppNotificationPushGateway pushGateway;
    private final EveAccountRepository eveAccountRepository;
    private final StructureRepository structureRepository;
    private final EsiGateway esiGateway;
    private final CacheGateway cacheGateway;

    /**
     * 势力铁壁 typeId（镜像 StructTask:42-47，QQ 类零 diff）
     */
    private static final Set<Long> FACTION_CITADEL_TYPE_IDS = Set.of(47513L, 47514L, 47515L, 47516L);

    /**
     * 检测建筑燃料不足事件并推送给有权用户。
     * <p>
     * 流程：分布式锁（600s）→ listAll distinct corpId → 逐军团查询 24h 内燃料耗尽建筑 →
     * 排除势力铁壁 → 全局去重（任一接收者行在窗口→整轮跳过）→ 权限扇出（Director/Station_Manager）→
     * 每接收者一行 insertBatch → 逐用户推送（在线判定；离线仅入库）。
     */
    public void detectFuelLow() {
        String lockKey = "app:notify:lock:FUEL_LOW";
        String uuid = UUID.randomUUID().toString();

        Boolean acquired = cacheGateway.setIfAbsent(lockKey, uuid, 600, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("燃料检测锁未获取，跳过本次执行");
            return;
        }

        try {
            List<Integer> corpIds = distinctCorpIds(eveAccountRepository.listAll());
            for (Integer corpId : corpIds) {
                try {
                    detectFuelLowForCorp(corpId);
                } catch (Exception e) {
                    log.error("燃料检测军团 {} 处理失败", corpId, e);
                }
            }
        } finally {
            cacheGateway.compareAndDelete(lockKey, uuid);
        }
    }

    /**
     * 单军团燃料检测。
     */
    private void detectFuelLowForCorp(Integer corpId) {
        List<Structure> structures = structureRepository.selectFuelExpiresList(24, corpId);
        if (structures == null || structures.isEmpty()) {
            return;
        }

        for (Structure structure : structures) {
            // 排除势力铁壁
            if (FACTION_CITADEL_TYPE_IDS.contains(structure.getTypeId())) {
                log.debug("跳过势力铁壁 structureId={}", structure.getStructureId());
                continue;
            }

            // 全局去重：任一接收者行在窗口→整轮跳过
            String dedupKey = "FUEL_LOW:" + structure.getStructureId();
            if (appNotificationRepository.existsByDedupKeyWithinHours(dedupKey, 24)) {
                log.debug("燃料检测去重跳过 structureId={}", structure.getStructureId());
                continue;
            }

            // 权限扇出
            List<Integer> recipients = fanout(corpId);
            if (recipients.isEmpty()) {
                log.debug("军团 {} 无有权接收者，跳过", corpId);
                continue;
            }

            // 构建通知行
            List<AppNotification> rows = new ArrayList<>();
            String title = truncate(AppNotificationEventType.FUEL_LOW.getDescription(), 128);
            String content = buildFuelContent(structure);

            Date now = new Date();
            for (Integer userId : recipients) {
                AppNotification row = new AppNotification();
                row.setUserId(userId);
                row.setEventType(AppNotificationEventType.FUEL_LOW.name());
                row.setDedupKey(dedupKey);
                row.setCharacterId(null);
                row.setCorpId(corpId);
                row.setTitle(title);
                row.setContent(content);
                row.setIsRead(false);
                row.setGmtCreate(now);
                rows.add(row);
            }

            // 先入库（id 回填）
            appNotificationRepository.insertBatch(rows);
            log.info("军团 {} 建筑 {} 燃料通知入库 {} 行", corpId, structure.getStructureId(), rows.size());

            // 后推送（离线仅入库）
            for (AppNotification row : rows) {
                pushIfOnline(row.getUserId(), row);
            }
        }
    }

    /**
     * 构建燃料通知正文。
     */
    private String buildFuelContent(Structure structure) {
        String name = structure.getName();
        OffsetDateTime fuelExpires = structure.getFuelExpires();
        String content;
        if (fuelExpires == null) {
            content = name + " 燃料已耗尽";
        } else {
            long hours = ChronoUnit.HOURS.between(OffsetDateTime.now(), fuelExpires);
            content = name + " 将在" + hours + "小时后燃料耗尽";
        }
        return truncate(content, 512);
    }

    /**
     * 权限扇出：返回军团内有权接收建筑通知的用户 ID 列表（Director/Station_Manager）。
     */
    private List<Integer> fanout(Integer corpId) {
        List<EveAccount> accounts = eveAccountRepository.listByCorpId(corpId);
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }

        Set<Integer> permitted = new HashSet<>();
        for (EveAccount account : accounts) {
            try {
                CharacterAccessTokenResult token = esiGateway.getAccessTokenWithExpiry(
                        account.getCharacterId(), account.getUserId());
                CharacterRolesInfo roles = esiGateway.queryCharacterRoles(
                        account.getCharacterId(), token.accessToken());
                if (roles != null && roles.hasStructurePermission()) {
                    permitted.add(account.getUserId());
                }
            } catch (Exception e) {
                log.error("角色 {} 权限查询失败", account.getCharacterId(), e);
            }
        }
        return new ArrayList<>(permitted);
    }

    // ─── 共用私有 helper（T032 复用） ─────────────────────

    /**
     * 字符串截断。
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /**
     * 在线推送（离线 no-op）。
     */
    private void pushIfOnline(Integer userId, AppNotification notification) {
        if (pushGateway.isOnline(userId)) {
            pushGateway.pushToUser(userId, envelope(notification));
        }
    }

    /**
     * 构建推送信封（contracts/rest-api.md:60）。
     */
    private String envelope(AppNotification n) {
        JSONObject data = new JSONObject();
        data.set("id", n.getId());
        data.set("eventType", n.getEventType());
        data.set("title", n.getTitle());
        data.set("content", n.getContent());
        data.set("characterId", n.getCharacterId());
        data.set("corpId", n.getCorpId());
        data.set("gmtCreate", n.getGmtCreate());

        JSONObject envelope = new JSONObject();
        envelope.set("type", "NOTIFICATION");
        envelope.set("data", data);
        return envelope.toString();
    }

    /**
     * 提取 distinct corpId 列表。
     */
    private List<Integer> distinctCorpIds(List<EveAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        return accounts.stream()
                .map(EveAccount::getCorpId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
