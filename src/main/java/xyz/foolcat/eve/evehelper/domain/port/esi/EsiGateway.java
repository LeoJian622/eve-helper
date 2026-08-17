package xyz.foolcat.eve.evehelper.domain.port.esi;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.text.ParseException;
import java.util.List;

/**
 * ESI 外部访问端口(防腐层契约)。
 * <p>
 * 领域层仅依赖本接口消费 ESI 数据,不直接 import 具体 ESI 客户端/模型/配置类。
 * 由基础设施层 {@code infrastructure/external/esi/EsiApiService} 实现。
 * <p>
 * 数据方法返回领域实体,datasource(国服 SERENITY)与响应模型转换由实现方封装。
 *
 * @author Leojan
 */
public interface EsiGateway {

    /**
     * 获取 ESI 授权 accessToken(按角色 ID 或认证码)。
     *
     * @param code   角色 ID 或认证码
     * @param userId 当前用户 ID
     * @return 带前缀的 accessToken
     * @throws ParseException JWT 解析失败
     */
    String getAccessToken(Integer code, Integer userId) throws ParseException;

    /**
     * 获取 ESI 授权 accessToken 及其剩余有效期。
     * <p>
     * 与 {@link #getAccessToken} 共用归属校验与并发锁,额外返回过期信息。
     * 缓存键构造封装在实现方,调用方不得自行拼装。
     *
     * @param characterId 角色 ID
     * @param userId      当前用户 ID
     * @return 含 accessToken、characterId 与剩余有效秒数的读模型
     * @throws ParseException JWT 解析失败
     */
    CharacterAccessTokenResult getAccessTokenWithExpiry(Integer characterId, Integer userId) throws ParseException;

    /**
     * 通过认证码换取 accessToken。
     *
     * @param code   认证码
     * @param userId 当前用户 ID
     * @return 带前缀的 accessToken
     * @throws ParseException JWT 解析失败
     */
    String authorize(String code, Integer userId) throws ParseException;

    /**
     * 判定绑定角色的 ESI 授权状态。
     *
     * @param account 绑定角色(需含 userId、refreshToken 与 characterId;userId 用于 accessToken 缓存键隔离)
     * @return ESI 授权状态
     */
    EsiAuthStatus getAuthorizationStatus(EveAccount account);

    // ── 资产 ──

    /**
     * 查询角色资产总页数。
     */
    Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken);

    /**
     * 查询角色指定页资产。
     */
    Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken);

    // ── 蓝图 ──

    /**
     * 查询军团蓝图总页数。
     */
    Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken);

    // ── 工业 ──

    /**
     * 查询军团工业任务总页数。
     */
    Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken);

    /**
     * 查询军团工业任务。
     */
    Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken);

    /**
     * 查询角色工业任务。
     */
    Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken);

    // ── 物品类型 ──

    /**
     * 查询物品类型信息。
     *
     * @param language 语言(如 zh)
     */
    Mono<InvTypes> queryUniverseType(Integer typeId, String language);

    // ── 采矿观察 ──

    /**
     * 查询军团采矿观察员总页数。
     */
    Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken);

    /**
     * 查询军团采矿观察员台账。
     */
    Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken);

    // ── 建筑 ──

    /**
     * 查询军团建筑总页数。
     */
    Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken);

    /**
     * 查询军团建筑。
     *
     * @param language 语言(如 zh)
     */
    Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken);

    // ── 名称解析 ──

    /**
     * 批量解析 ID 对应的名称。
     */
    Flux<UniverseName> queryUniverseNames(List<Integer> ids);

    // ── 钱包流水 ──

    /**
     * 查询军团钱包流水总页数。
     */
    Integer queryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken);

    /**
     * 查询军团钱包流水。
     */
    Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken);

    /**
     * 查询人物钱包流水总页数(单分账)。
     */
    Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken);

    /**
     * 查询人物钱包流水指定页(单分账)。
     */
    Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken);
}