package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.service.system.MiningDetailService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

/**
 * 月矿采掘应用服务
 * 负责月矿堡开采明细同步用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiningApplicationService {

    private final MiningDetailService miningDetailService;

    private final AccessGuard accessGuard;

    /**
     * 同步指定角色的月矿堡开采明细。
     * <p>
     * 访问控制:characterId 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)。
     *
     * @param characterId 人物 ID
     * @param observerId  月矿堡 ID
     */
    public void syncMiningByObserver(Integer characterId, Long observerId) {
        accessGuard.requireOwnership(String.valueOf(characterId), "月矿开采明细同步");
        try {
            miningDetailService.saveObserverMining(characterId, observerId);
        } catch (ParseException e) {
            log.error("采矿明细同步失败: characterId={}, observerId={}", characterId, observerId, e);
            throw new EveHelperException("采矿明细同步失败", e);
        }
    }
}