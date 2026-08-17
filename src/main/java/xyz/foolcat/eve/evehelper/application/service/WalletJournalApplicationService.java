package xyz.foolcat.eve.evehelper.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletJournalAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletJournalVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.text.ParseException;

/**
 * 钱包流水应用服务。
 * <p>提供人物钱包 journal 的手动同步与分页查询用例:
 * 两个入口均在业务逻辑前先做归属校验(accessGuard.requireOwnership),防御 IDOR。</p>
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletJournalApplicationService {

    private final AccessGuard accessGuard;
    private final WalletJournalService walletJournalService;
    private final WalletJournalRepository walletJournalRepository;
    private final WalletJournalAssembler walletJournalAssembler;

    /**
     * 手动同步某人物钱包流水(ESI -> DB,幂等 upsert)。
     *
     * @param cid 人物ID
     */
    public void syncCharacterJournal(Integer cid) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包流水同步");
        try {
            walletJournalService.syncCharacterJournal(cid);
        } catch (ParseException e) {
            log.error("钱包流水同步失败: cid={}", cid, e);
            throw new EveHelperException("钱包流水同步失败", e);
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
    public PageResult<WalletJournalVO> queryPage(String cid, int current, int size) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(cid, "钱包流水");
        IPage<WalletJournalPO> page = new Page<>(current, size);
        IPage<WalletJournal> domainPage = walletJournalRepository.selectPageByOwnerId(page, Integer.valueOf(cid));
        return PageResultUtil.copy(domainPage, walletJournalAssembler::toVo);
    }
}