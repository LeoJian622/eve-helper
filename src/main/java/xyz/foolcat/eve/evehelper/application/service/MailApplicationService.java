package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.dto.request.SendMailRequest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailRecipient;
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
import xyz.foolcat.eve.evehelper.domain.model.vo.SendMailCommand;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;

/**
 * 邮件应用服务(022 US2)。
 *
 * <p>提供六个用例:收件箱({@link #inbox})、标签({@link #labels})、
 * 详情({@link #detail})、标记已读({@link #markRead})、发送({@link #send})、
 * 名称解析({@link #resolveNames})。</p>
 *
 * <p>关键设计:</p>
 * <ul>
 *     <li>缓存/强刷在网关层;应用层不做结果缓存(YAGNI)</li>
 *     <li>异常原样传播:EsiAccessException(含 permissionLow)/authorize 异常不吞不包装</li>
 *     <li>resolveNames 公开:无 authorize、无 token,直调 resolveUniverseIds</li>
 *     <li>SendMailRequest→SendMailCommand 映射在应用层</li>
 * </ul>
 *
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
public class MailApplicationService {

    private final AuthorizeUtil authorizeUtil;
    private final EsiGateway esiGateway;

    /**
     * 收件箱:透传 lastMailId 游标参数(首页传 null)。
     *
     * @param characterId  角色 ID
     * @param lastMailId   游标;首页传 null
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 邮件标题分页
     * @throws ParseException JWT 解析失败
     */
    public MailHeaderPage inbox(Integer characterId, Integer lastMailId, boolean forceRefresh) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        return esiGateway.queryCharacterMailHeaders(characterId, lastMailId, accessToken, forceRefresh);
    }

    /**
     * 标签:返回标签列表与总未读数。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 邮件标签与未读数
     * @throws ParseException JWT 解析失败
     */
    public MailLabelsInfo labels(Integer characterId, boolean forceRefresh) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        return esiGateway.queryCharacterMailLabels(characterId, accessToken, forceRefresh);
    }

    /**
     * 详情:返回原始正文(不转换不执行)。
     *
     * @param characterId  角色 ID
     * @param mailId       邮件 ID
     * @param forceRefresh true 时强制刷新(透传网关);false 走缓存
     * @return 邮件详情
     * @throws ParseException JWT 解析失败
     */
    public MailDetailInfo detail(Integer characterId, Integer mailId, boolean forceRefresh) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        return esiGateway.queryCharacterMail(characterId, mailId, accessToken, forceRefresh);
    }

    /**
     * 标记已读/未读:委托网关。
     *
     * @param characterId 角色 ID
     * @param mailId      邮件 ID
     * @param read        true=已读,false=未读
     * @throws ParseException JWT 解析失败
     */
    public void markRead(Integer characterId, Integer mailId, boolean read) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        esiGateway.markCharacterMailRead(characterId, mailId, read, accessToken);
    }

    /**
     * 发送邮件:SendMailRequest→SendMailCommand 映射后委托网关。
     * <p>
     * 权限不足(EsiAccessException.permissionLow)原样透传,不包装新错误码。
     *
     * @param characterId 角色 ID
     * @param request     发送邮件请求 DTO
     * @return 新邮件 ID
     * @throws ParseException JWT 解析失败
     */
    public Integer send(Integer characterId, SendMailRequest request) throws ParseException {
        EveAccount account = authorizeUtil.authorize(characterId);
        CharacterAccessTokenResult tokenResult = esiGateway.getAccessTokenWithExpiry(characterId, account.getUserId());
        String accessToken = tokenResult.accessToken();

        // SendMailRequest → SendMailCommand 映射
        List<MailRecipient> recipients = request.recipients().stream()
                .map(r -> new MailRecipient(r.recipientId(), r.recipientType()))
                .toList();
        SendMailCommand command = new SendMailCommand(recipients, request.subject(), request.body());

        return esiGateway.sendCharacterMail(characterId, command, accessToken);
    }

    /**
     * 名称解析(公开,无授权):直调 resolveUniverseIds。
     *
     * @param names 名称列表
     * @return 解析结果(全量,含未命中项)
     */
    public List<RecipientResolution> resolveNames(List<String> names) {
        return esiGateway.resolveUniverseIds(names);
    }
}
