package xyz.foolcat.eve.evehelper.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletJournalAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletJournalVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

/**
 * 钱包流水应用服务。
 * <p>提供人物/军团钱包 journal 的手动同步与分页查询用例:
 * 各入口均在业务逻辑前先做归属校验(accessGuard.requireOwnership),防御 IDOR。</p>
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletJournalApplicationService {

    /** 同步冷却键前缀(同一 owner 冷却期内不允许重复同步,防 ESI 限流滥用) */
    private static final String SYNC_COOLDOWN_PREFIX = "wallet:journal:sync:";

    /** 同步冷却秒数;测试 profile 设为 0 即禁用冷却 */
    @Value("${eve-helper.sync.cooldown-seconds:60}")
    private long syncCooldownSeconds;

    private final AccessGuard accessGuard;
    private final WalletJournalService walletJournalService;
    private final WalletJournalRepository walletJournalRepository;
    private final WalletJournalAssembler walletJournalAssembler;
    private final CacheGateway cacheGateway;

    /**
     * 手动同步某人物钱包流水(ESI -> DB,幂等 upsert)。
     *
     * @param cid 人物ID
     */
    public void syncCharacterJournal(Integer cid) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包流水同步");
        // 冷却限流:同一人物冷却期内不允许重复同步,防 ESI 限流滥用
        String cooldownKey = SYNC_COOLDOWN_PREFIX + "char:" + cid;
        requireSyncCooldown(cooldownKey);
        try {
            walletJournalService.syncCharacterJournal(cid);
        } catch (ParseException e) {
            log.error("钱包流水同步失败: cid={}", cid, e);
            throw new EveHelperException("钱包流水同步失败", e);
        }
    }

    /**
     * 手动同步某角色关联军团的钱包流水(1..7 分账,幂等 upsert,分账级失败隔离)。
     *
     * <p><b>013 US1</b>:入参统一为<b>角色ID(characterId)</b>;归属校验(requireOwnership)基于该角色
     * 是否属于当前用户(防 IDOR),目标军团ID由领域服务从该角色 eve_account 行派生 —— 调用方无法指定任意军团。</p>
     *
     * @param characterId 角色ID(该角色的 eve_account 行须含关联军团)
     */
    public void syncCorporationJournal(Integer characterId) {
        // 归属校验先于业务逻辑:characterId 为用户可控入参,须先确认该角色属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(characterId), "军团钱包流水同步");
        // 冷却限流:同一角色冷却期内不允许重复同步,防 ESI 限流滥用
        String cooldownKey = SYNC_COOLDOWN_PREFIX + "corp:" + characterId;
        requireSyncCooldown(cooldownKey);
        try {
            walletJournalService.syncCorporationJournal(characterId);
        } catch (ParseException e) {
            log.error("军团钱包流水同步失败: characterId={}", characterId, e);
            throw new EveHelperException("军团钱包流水同步失败", e);
        }
    }

    /**
     * 分页查询某人物钱包流水(时间 date 倒序,真实 IPage 物理分页,total 正确)。
     *
     * @param cid     人物ID
     * @param current 页码(从 1 开始)
     * @param size    每页行数
     * @return 钱包流水视图分页结果
     */
    public PageResult<WalletJournalVO> queryPage(Integer cid, int current, int size) {
        // 入参边界校验先于归属鉴定:size<1 会绕过 MAX_PAGE_SIZE 上限导致私有流水分页全量返回,current<1 产生非法 LIMIT,current>10000 产生超大偏移
        if (cid == null || current < 1 || current > 10000 || size < 1 || size > 1000) {
            throw new EveHelperException("分页参数不合法");
        }
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包流水");
        IPage<WalletJournal> page = new Page<>(current, size);
        IPage<WalletJournal> domainPage = walletJournalRepository.selectPageByOwnerId(page, cid.longValue());
        return PageResultUtil.copy(domainPage, walletJournalAssembler::toVo);
    }

    /**
     * 分页查询某军团某分账钱包流水(时间 date 倒序,真实 IPage 物理分页,total 正确)。
     * <p>入参边界(division 1..7、current/size)先于归属校验执行,防越权探测/防泄漏。</p>
     *
     * @param corpId   军团ID
     * @param division 军团分账(1..7)
     * @param current  页码(从 1 开始)
     * @param size     每页行数
     * @return 钱包流水视图分页结果
     */
    public PageResult<WalletJournalVO> queryCorporationPage(Integer corpId, Integer division,
                                                            int current, int size) {
        // 入参边界校验先于归属鉴定:division 越界或分页越界即拒绝,防越权探测/防私有数据泄漏
        if (corpId == null || division == null || division < 1 || division > 7) {
            throw new EveHelperException("军团分账参数不合法");
        }
        if (current < 1 || current > 10000 || size < 1 || size > 1000) {
            throw new EveHelperException("分页参数不合法");
        }
        // 军团维度读过滤(US2b):corporationScope 返回当前同步者 userId(ROOT=null 不过滤看全量),
        // 透传仓储按 user_id 过滤,只有同步者私有军团数据可见(推翻军团成员共享)
        Long scope = accessGuard.corporationScope("军团钱包流水");
        IPage<WalletJournal> page = new Page<>(current, size);
        IPage<WalletJournal> domainPage =
                walletJournalRepository.selectPageByOwnerAndDivision(page, corpId.longValue(), division, scope);
        return PageResultUtil.copy(domainPage, walletJournalAssembler::toVo);
    }

    /* ────────────────────────── 同步限流 ────────────────────────── */

    /**
     * 同步冷却校验:若冷却键已存在则拒绝(冷却期内重复同步),否则设置冷却键。
     *
     * @param cooldownKey Redis 冷却键(如 {@code wallet:journal:sync:char:9001})
     */
    private void requireSyncCooldown(String cooldownKey) {
        if (syncCooldownSeconds <= 0) {
            return; // 冷却已禁用(如测试 profile)
        }
        Boolean acquired = cacheGateway.setIfAbsent(cooldownKey, "1", syncCooldownSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new EveHelperException("同步操作过于频繁，请稍后再试");
        }
    }
}