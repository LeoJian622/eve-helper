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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAttributesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterLocationInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterOnlineInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPublicInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillList;
import xyz.foolcat.eve.evehelper.domain.model.vo.EsiAuthorizationDetail;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
import xyz.foolcat.eve.evehelper.domain.model.vo.SendMailCommand;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
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

    /**
     * 判定绑定角色的 ESI 授权状态,并在 AUTHORIZED 时解析已授权权限(scopes)。
     * <p>
     * scopes 取自当前有效 accessToken(缓存或本次判定中刷新所得)的已授权权限声明;
     * 非 AUTHORIZED、无可用 accessToken 或解析失败时 scopes 为 null
     * (空列表表示 token 有效但未声明任何受限权限)。
     * 不得因本方法产生额外的上游 ESI 调用或 refreshToken 刷新。
     *
     * @param account 绑定角色(需含 userId、refreshToken 与 characterId)
     * @return 授权状态 + 已授权权限(可 null)
     */
    EsiAuthorizationDetail getAuthorizationDetail(EveAccount account);

    // ── 资产 ──

    /**
     * 查询角色资产总页数。
     */
    Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken);

    /**
     * 查询角色资产总页数(可选强制刷新)。
     *
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     */
    Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色指定页资产。
     */
    Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken);

    /**
     * 查询角色指定页资产(可选强制刷新)。
     *
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     */
    Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken, boolean forceRefresh);

    // ── 蓝图 ──

    /**
     * 查询军团蓝图总页数。
     */
    Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken);

    /**
     * 查询军团蓝图总页数(可选强制刷新)。
     */
    Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken, boolean forceRefresh);

    // ── 工业 ──

    /**
     * 查询军团工业任务总页数。
     */
    Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken);

    /**
     * 查询军团工业任务总页数(可选强制刷新)。
     */
    Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh);

    /**
     * 查询军团工业任务。
     */
    Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken);

    /**
     * 查询军团工业任务(可选强制刷新)。
     */
    Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh);

    /**
     * 查询角色工业任务。
     */
    Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken);

    /**
     * 查询角色工业任务(可选强制刷新)。
     */
    Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken, boolean forceRefresh);

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
     * 查询军团采矿观察员总页数(可选强制刷新)。
     */
    Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken, boolean forceRefresh);

    /**
     * 查询军团采矿观察员台账。
     */
    Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken);

    /**
     * 查询军团采矿观察员台账(可选强制刷新)。
     */
    Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken, boolean forceRefresh);

    // ── 建筑 ──

    /**
     * 查询军团建筑总页数。
     */
    Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken);

    /**
     * 查询军团建筑总页数(可选强制刷新)。
     */
    Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken, boolean forceRefresh);

    /**
     * 查询军团建筑。
     *
     * @param language 语言(如 zh)
     */
    Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken);

    /**
     * 查询军团建筑(可选强制刷新)。
     */
    Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken, boolean forceRefresh);

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
     * 查询军团钱包流水总页数(可选强制刷新)。
     */
    Integer queryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken, boolean forceRefresh);

    /**
     * 查询军团钱包流水。
     */
    Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken);

    /**
     * 查询军团钱包流水(可选强制刷新)。
     */
    Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken, boolean forceRefresh);

    /**
     * 查询人物钱包流水总页数(单分账)。
     */
    Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken);

    /**
     * 查询人物钱包流水总页数(单分账,可选强制刷新)。
     */
    Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询人物钱包流水指定页(单分账)。
     */
    Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken);

    /**
     * 查询人物钱包流水指定页(单分账,可选强制刷新)。
     */
    Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken, boolean forceRefresh);

    // ── 钱包交易 ──

    /**
     * 查询人物钱包交易(from_id 游标,首页传 null 表示最新)。
     *
     * @param characterId 角色 ID
     * @param fromId      游标,返回该 ID 之前的记录;首页传 null
     * @param accessToken 授权 Token
     */
    Flux<WalletTransaction> queryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken);

    /**
     * 查询人物钱包交易(可选强制刷新)。
     */
    Flux<WalletTransaction> queryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken, boolean forceRefresh);

    /**
     * 查询军团钱包交易(from_id 游标,首页传 null 表示最新)。
     *
     * @param corporationId 军团 ID
     * @param division      军团钱包分账
     * @param fromId        游标,返回该 ID 之前的记录;首页传 null
     * @param accessToken   授权 Token
     */
    Flux<WalletTransaction> queryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken);

    /**
     * 查询军团钱包交易(可选强制刷新)。
     */
    Flux<WalletTransaction> queryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken, boolean forceRefresh);

    // ── 钱包余额(直查) ──

    /**
     * 查询人物钱包当前余额(直调 ESI /characters/{id}/wallet/)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 当前余额(ISK)
     */
    Double queryCharacterWalletBalance(Integer characterId, String accessToken);

    /**
     * 查询军团钱包各分账余额(直调 ESI /corporations/{id}/wallets/)。
     *
     * @param corporationId 军团 ID
     * @param accessToken   授权 Token
     * @return 各分账余额列表(1..7,但 ESI 仅返回有数据的分账)
     */
    List<WalletDivisionBalance> queryCorporationWalletBalances(Integer corporationId, String accessToken);

    // ── 角色面板(伴侣 APP) ──

    /**
     * 查询角色公开信息(ESI /characters/{id}/,无需授权)。
     *
     * @param characterId 角色 ID
     * @return 角色公开信息
     */
    CharacterPublicInfo queryCharacterPublicInfo(Integer characterId);

    /**
     * 查询角色公开信息(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色公开信息
     */
    CharacterPublicInfo queryCharacterPublicInfo(Integer characterId, boolean forceRefresh);

    /**
     * 查询角色在线状态(ESI /characters/{id}/online/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 角色在线状态
     */
    CharacterOnlineInfo queryCharacterOnline(Integer characterId, String accessToken);

    /**
     * 查询角色在线状态(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色在线状态
     */
    CharacterOnlineInfo queryCharacterOnline(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色当前位置(ESI /characters/{id}/location/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 角色当前位置
     */
    CharacterLocationInfo queryCharacterLocation(Integer characterId, String accessToken);

    /**
     * 查询角色当前位置(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色当前位置
     */
    CharacterLocationInfo queryCharacterLocation(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色当前舰船类型 ID(ESI /characters/{id}/ship/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 舰船类型 ID
     */
    Integer queryCharacterShipTypeId(Integer characterId, String accessToken);

    /**
     * 查询角色当前舰船类型 ID(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 舰船类型 ID
     */
    Integer queryCharacterShipTypeId(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色技能列表(ESI /characters/{id}/skills/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 角色技能列表
     */
    CharacterSkillList queryCharacterSkills(Integer characterId, String accessToken);

    /**
     * 查询角色技能列表(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色技能列表
     */
    CharacterSkillList queryCharacterSkills(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色属性(ESI /characters/{id}/attributes/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 角色属性
     */
    CharacterAttributesInfo queryCharacterAttributes(Integer characterId, String accessToken);

    /**
     * 查询角色属性(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色属性
     */
    CharacterAttributesInfo queryCharacterAttributes(Integer characterId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色技能队列(ESI /characters/{id}/skillqueue/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 技能队列(空队列返回空列表,查询失败抛 EsiAccessException)
     */
    List<SkillQueueItem> queryCharacterSkillQueue(Integer characterId, String accessToken);

    /**
     * 查询角色技能队列(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 技能队列(空队列返回空列表,查询失败抛 EsiAccessException)
     */
    List<SkillQueueItem> queryCharacterSkillQueue(Integer characterId, String accessToken, boolean forceRefresh);

    // ── 邮件读侧(伴侣 APP T016) ──

    /**
     * 查询角色邮件标题(ESI /characters/{id}/mail/,需授权)。
     *
     * @param characterId 角色 ID
     * @param lastMailId  游标(返回该 ID 之前的邮件);首页传 null
     * @param accessToken 授权 Token
     * @return 邮件标题分页(含 nextLastMailId 游标)
     */
    MailHeaderPage queryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken);

    /**
     * 查询角色邮件标题(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param lastMailId   游标;首页传 null
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 邮件标题分页
     */
    MailHeaderPage queryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色邮件详情(ESI /characters/{id}/mail/{mailId}/,需授权)。
     *
     * @param characterId 角色 ID
     * @param mailId      邮件 ID
     * @param accessToken 授权 Token
     * @return 邮件详情
     */
    MailDetailInfo queryCharacterMail(Integer characterId, Integer mailId, String accessToken);

    /**
     * 查询角色邮件详情(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param mailId       邮件 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 邮件详情
     */
    MailDetailInfo queryCharacterMail(Integer characterId, Integer mailId, String accessToken, boolean forceRefresh);

    /**
     * 查询角色邮件标签(ESI /characters/{id}/mail/labels/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 邮件标签与未读数
     */
    MailLabelsInfo queryCharacterMailLabels(Integer characterId, String accessToken);

    /**
     * 查询角色邮件标签(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 邮件标签与未读数
     */
    MailLabelsInfo queryCharacterMailLabels(Integer characterId, String accessToken, boolean forceRefresh);

    // ── 邮件写侧(伴侣 APP T017) ──

    /**
     * 标记角色邮件为已读/未读(ESI PUT /characters/{id}/mail/{mailId}/,需授权)。
     * <p>写操作不做响应缓存、无 forceRefresh。
     *
     * @param characterId 角色 ID
     * @param mailId      邮件 ID
     * @param read        true=已读,false=未读
     * @param accessToken 授权 Token
     */
    void markCharacterMailRead(Integer characterId, Integer mailId, boolean read, String accessToken);

    /**
     * 发送角色邮件(ESI POST /characters/{id}/mail/,需授权)。
     * <p>写操作不做响应缓存、无 forceRefresh。
     *
     * @param characterId 角色 ID
     * @param command     发送命令(收件人、主题、正文)
     * @param accessToken 授权 Token
     * @return 新邮件 ID
     */
    Integer sendCharacterMail(Integer characterId, SendMailCommand command, String accessToken);

    // ── universe/ids 精确名称解析(伴侣 APP T018) ──

    /**
     * 批量解析名称→ID(ESI POST /universe/ids/,公开无需授权)。
     * <p>
     * 公开数据:pub 段共享(userId=null)、不限流、无 forceRefresh。
     * names 先排序再确定性编码进缓存键(顺序无关命中)。
     * 响应按 characters/corporations/alliances 分组映射;
     * 请求中未命中的名称返回 {@code resolved=false} 条目(保持请求名全量返回)。
     *
     * @param names 名称列表
     * @return 解析结果(全量,含未命中项)
     */
    List<RecipientResolution> resolveUniverseIds(List<String> names);

    // ── 角色军团权限(伴侣 APP T029) ──

    /**
     * 查询角色军团权限(ESI /characters/{id}/roles/,需授权)。
     *
     * @param characterId 角色 ID
     * @param accessToken 授权 Token
     * @return 角色军团权限(三集合 + 权限判定助手)
     */
    CharacterRolesInfo queryCharacterRoles(Integer characterId, String accessToken);

    /**
     * 查询角色军团权限(可选强制刷新)。
     *
     * @param characterId  角色 ID
     * @param accessToken  授权 Token
     * @param forceRefresh true 时忽略缓存真调(受强刷限流);false 默认走缓存
     * @return 角色军团权限
     */
    CharacterRolesInfo queryCharacterRoles(Integer characterId, String accessToken, boolean forceRefresh);
}