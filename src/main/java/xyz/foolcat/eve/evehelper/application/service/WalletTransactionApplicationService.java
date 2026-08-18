package xyz.foolcat.eve.evehelper.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletTransactionAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletTransactionVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletTransactionService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 钱包交易应用服务。
 * <p>提供人物钱包交易(transaction)的手动同步与分页查询用例,以及军团钱包交易(1..7 分账)
 * 的同步与分账分页查询用例:各入口均在业务逻辑前先做归属校验(accessGuard.requireOwnership),
 * 防御 IDOR。</p>
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTransactionApplicationService {

    /** 同步冷却键前缀(同一 owner 冷却期内不允许重复同步,防 ESI 限流滥用) */
    private static final String SYNC_COOLDOWN_PREFIX = "wallet:sync:";

    /** 同步冷却秒数;测试 profile 设为 0 即禁用冷却 */
    @org.springframework.beans.factory.annotation.Value("${eve-helper.sync.cooldown-seconds:60}")
    private long syncCooldownSeconds;

    private final AccessGuard accessGuard;
    private final WalletTransactionService walletTransactionService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionAssembler walletTransactionAssembler;
    private final CacheGateway cacheGateway;

    /**
     * 手动同步某人物钱包交易(ESI -> DB,幂等 upsert)。
     *
     * @param cid 人物ID
     */
    public void syncCharacterTransactions(Integer cid) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包交易同步");
        // 冷却限流:同一人物 60 秒内不允许重复同步,防 ESI 限流滥用
        String cooldownKey = SYNC_COOLDOWN_PREFIX + "char:" + cid;
        requireSyncCooldown(cooldownKey);
        try {
            walletTransactionService.syncCharacterTransactions(cid);
        } catch (ParseException e) {
            log.error("钱包交易同步失败: cid={}", cid, e);
            throw new EveHelperException("钱包交易同步失败", e);
        }
    }

    /**
     * 分页查询某人物钱包交易(时间 date 倒序,真实 IPage 物理分页,total 正确)。
     *
     * @param cid     人物ID
     * @param current 页码(从 1 开始)
     * @param size    每页行数
     * @return 钱包交易视图分页结果
     */
    public PageResult<WalletTransactionVO> queryCharacterPage(Integer cid, int current, int size) {
        // 入参边界校验先于归属鉴定:size<1 会绕过 MAX_PAGE_SIZE 上限导致私有交易分页全量返回,current<1 产生非法 LIMIT
        if (cid == null || current < 1 || size < 1 || size > 1000) {
            throw new EveHelperException("分页参数不合法");
        }
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包交易");
        IPage<WalletTransaction> page = new Page<>(current, size);
        // 人物侧 ownerType="character", division=0;ownerId = cid.longValue()
        IPage<WalletTransaction> domainPage =
                walletTransactionRepository.selectPageByOwner(page, "character", cid.longValue(), 0);
        return PageResultUtil.copy(domainPage, walletTransactionAssembler::toVo);
    }

    /**
     * 手动同步某军团钱包交易(1..7 分账,幂等 upsert,分账级失败隔离)。
     * <p>任一分账 ESI 失败由领域服务汇总抛出(含失败 division 明细),但已成功分账已落库、不回滚;
     * 仅全部成功时正常返回 division→是否成功映射。</p>
     *
     * @param corpId 军团ID
     * @return division→是否同步成功(仅全部成功时返回)
     */
    public Map<Integer, Boolean> syncCorporationTransactions(Integer corpId) {
        // 归属校验先于业务逻辑:corpId 为用户可控入参,须先确认该军团属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(corpId), "军团钱包交易同步");
        // 冷却限流:同一军团 60 秒内不允许重复同步,防 ESI 限流滥用
        String cooldownKey = SYNC_COOLDOWN_PREFIX + "corp:" + corpId;
        requireSyncCooldown(cooldownKey);
        try {
            return walletTransactionService.syncCorporationTransactions(corpId);
        } catch (ParseException e) {
            log.error("军团钱包交易同步失败: corpId={}", corpId, e);
            throw new EveHelperException("军团钱包交易同步失败", e);
        }
    }

    /**
     * 分页查询某军团某分账钱包交易(时间 date 倒序,真实 IPage 物理分页,total 正确)。
     * <p>入参边界(division 1..7、current/size)先于归属校验执行,防越权探测/防泄漏。</p>
     *
     * @param corpId   军团ID
     * @param division 军团分账(1..7)
     * @param current  页码(从 1 开始)
     * @param size     每页行数
     * @return 钱包交易视图分页结果
     */
    public PageResult<WalletTransactionVO> queryCorporationPage(Integer corpId, Integer division,
                                                                int current, int size) {
        // 入参边界校验先于归属鉴定:division 越界或分页越界即拒绝,防越权探测/防私有数据泄漏
        if (corpId == null || division == null || division < 1 || division > 7) {
            throw new EveHelperException("军团分账参数不合法");
        }
        if (current < 1 || size < 1 || size > 1000) {
            throw new EveHelperException("分页参数不合法");
        }
        // 归属校验先于业务逻辑:corpId 为用户可控入参,须先确认该军团属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(corpId), "军团钱包交易");
        IPage<WalletTransaction> page = new Page<>(current, size);
        // 军团侧 ownerType="corporation",division=1..7;ownerId = corpId.longValue()
        IPage<WalletTransaction> domainPage =
                walletTransactionRepository.selectPageByOwner(page, "corporation", corpId.longValue(), division);
        return PageResultUtil.copy(domainPage, walletTransactionAssembler::toVo);
    }

    /* ────────────────────────── 同步限流 ────────────────────────── */

    /**
     * 同步冷却校验:若冷却键已存在则拒绝(60 秒内重复同步),否则设置冷却键。
     *
     * @param cooldownKey Redis 冷却键(如 {@code wallet:sync:char:9001})
     */
    private void requireSyncCooldown(String cooldownKey) {
        if (syncCooldownSeconds <= 0) {
            return; // 冷却已禁用(如测试 profile)
        }
        if (Boolean.TRUE.equals(cacheGateway.hasKey(cooldownKey))) {
            throw new EveHelperException("同步操作过于频繁，请稍后再试");
        }
        cacheGateway.set(cooldownKey, "1", syncCooldownSeconds, TimeUnit.SECONDS);
    }
}