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
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletTransactionService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.text.ParseException;

/**
 * 钱包交易应用服务。
 * <p>提供人物钱包交易(transaction)的手动同步与分页查询用例:
 * 两个入口均在业务逻辑前先做归属校验(accessGuard.requireOwnership),防御 IDOR。</p>
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTransactionApplicationService {

    private final AccessGuard accessGuard;
    private final WalletTransactionService walletTransactionService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionAssembler walletTransactionAssembler;

    /**
     * 手动同步某人物钱包交易(ESI -> DB,幂等 upsert)。
     *
     * @param cid 人物ID
     */
    public void syncCharacterTransactions(Integer cid) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包交易同步");
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
}