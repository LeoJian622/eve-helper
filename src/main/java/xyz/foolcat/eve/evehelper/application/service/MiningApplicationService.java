package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.service.system.MiningDetailService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

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
     * @param characterId  人物 ID
     * @param observerId   月矿堡 ID
     * @param forceRefresh 是否强制刷新(忽略缓存)
     */
    public void syncMiningByObserver(Integer characterId, Long observerId, boolean forceRefresh) {
        // observerId 本地正整数校验：防御空值/负值穿透到下游 ESI 请求
        // （ESI 上游会拒绝，但本地先拒绝可减少无效网络调用与日志噪声）
        if (observerId == null || observerId <= 0) {
            log.warn("月矿开采明细同步参数非法：observerId={}", observerId);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        accessGuard.requireOwnership(String.valueOf(characterId), "月矿开采明细同步");
        try {
            miningDetailService.saveObserverMining(characterId, observerId, forceRefresh);
        } catch (ParseException e) {
            log.error("采矿明细同步失败: characterId={}, observerId={}", characterId, observerId, e);
            throw new EveHelperException("采矿明细同步失败", e);
        }
    }
}