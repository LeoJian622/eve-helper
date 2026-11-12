package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.dto.request.SendMailRequest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailRecipient;
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
import xyz.foolcat.eve.evehelper.domain.model.vo.SendMailCommand;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MailApplicationService 单元测试（TDD RED, 022-T014）。
 *
 * <p>纯 Mockito 单测,固化 US2 邮件应用服务的 8 个行为契约。
 * 本任务仅写失败测试——MailApplicationService 与 SendMailRequest DTO
 * 由 T019 创建,编译失败本身即 RED 证据。</p>
 *
 * <p>用例:
 * <ol>
 *   <li>inbox_passesLastMailIdCursor</li>
 *   <li>labels_returnsTotalUnread</li>
 *   <li>detail_returnsRawBody</li>
 *   <li>markRead_delegatesToGateway</li>
 *   <li>send_success_returnsMailId</li>
 *   <li>send_permissionLow_throwsReauthHint (US2-4)</li>
 *   <li>send_otherUsersCharacter_rejected (US2-5)</li>
 *   <li>resolveNames_partialUnresolved_marksResolvedFalse (US2-7)</li>
 * </ol>
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailApplicationService 邮件应用服务(022 US2)")
class MailApplicationServiceTest {

    private static final int CHARACTER_ID = 95465499;
    private static final int USER_ID = 1;
    private static final int MAIL_ID = 123456;
    private static final String STUB_ACCESS_TOKEN = "Bearer stub-mail-token";

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EsiGateway esiGateway;

    // ── 辅助方法 ──

    private EveAccount buildAccount() {
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setCharacterName("MailTestChar");
        account.setUserId(USER_ID);
        return account;
    }

    private void stubAuthorize() {
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(buildAccount());
    }

    private void stubAccessToken() throws ParseException {
        when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID))
                .thenReturn(new CharacterAccessTokenResult(STUB_ACCESS_TOKEN, CHARACTER_ID, 1140L));
    }

    /* ============================ US2 邮件读侧 ============================ */

    @Test
    @DisplayName("inbox 透传 lastMailId 游标参数")
    void inbox_passesLastMailIdCursor() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        Integer lastMailId = 999999;
        MailHeaderPage stubPage = new MailHeaderPage(
                List.of(new MailHeaderPage.MailHeaderItem(
                        MAIL_ID, 10001, "Test Subject",
                        OffsetDateTime.parse("2026-11-12T10:00:00Z"), false, List.of(1))),
                999998
        );
        when(esiGateway.queryCharacterMailHeaders(eq(CHARACTER_ID), eq(lastMailId),
                eq(STUB_ACCESS_TOKEN), anyBoolean()))
                .thenReturn(stubPage);

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        MailHeaderPage result = service.inbox(CHARACTER_ID, lastMailId, false);

        assertThat(result.mails()).hasSize(1);
        assertThat(result.nextLastMailId()).isEqualTo(999998);
        verify(esiGateway).queryCharacterMailHeaders(CHARACTER_ID, lastMailId, STUB_ACCESS_TOKEN, false);
    }

    @Test
    @DisplayName("labels 返回标签列表与总未读数")
    void labels_returnsTotalUnread() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        MailLabelsInfo stubLabels = new MailLabelsInfo(
                List.of(
                        new MailLabelsInfo.MailLabel(1, "Inbox", 5L),
                        new MailLabelsInfo.MailLabel(2, "Corp", 3L)
                ),
                8L
        );
        when(esiGateway.queryCharacterMailLabels(eq(CHARACTER_ID), eq(STUB_ACCESS_TOKEN), anyBoolean()))
                .thenReturn(stubLabels);

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        MailLabelsInfo result = service.labels(CHARACTER_ID, false);

        assertThat(result.totalUnread()).isEqualTo(8L);
        assertThat(result.labels()).hasSize(2);
        verify(esiGateway).queryCharacterMailLabels(CHARACTER_ID, STUB_ACCESS_TOKEN, false);
    }

    @Test
    @DisplayName("detail 返回原始正文(不转换不执行)")
    void detail_returnsRawBody() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        String rawHtmlBody = "<p>Original <b>HTML</b> body — 不转换</p>";
        MailDetailInfo stubDetail = new MailDetailInfo(
                MAIL_ID, 10001,
                List.of(new MailRecipient(20001, "character")),
                "Detail Subject",
                OffsetDateTime.parse("2026-11-12T10:00:00Z"),
                rawHtmlBody, false
        );
        when(esiGateway.queryCharacterMail(eq(CHARACTER_ID), eq(MAIL_ID),
                eq(STUB_ACCESS_TOKEN), anyBoolean()))
                .thenReturn(stubDetail);

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        MailDetailInfo result = service.detail(CHARACTER_ID, MAIL_ID, false);

        assertThat(result.body()).isEqualTo(rawHtmlBody);
        assertThat(result.mailId()).isEqualTo(MAIL_ID);
        verify(esiGateway).queryCharacterMail(CHARACTER_ID, MAIL_ID, STUB_ACCESS_TOKEN, false);
    }

    @Test
    @DisplayName("markRead 委托网关标记已读/未读")
    void markRead_delegatesToGateway() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        service.markRead(CHARACTER_ID, MAIL_ID, true);

        verify(esiGateway).markCharacterMailRead(CHARACTER_ID, MAIL_ID, true, STUB_ACCESS_TOKEN);
    }

    /* ============================ US2 邮件写侧 ============================ */

    @Test
    @DisplayName("send 成功返回新邮件 ID")
    void send_success_returnsMailId() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        Integer newMailId = 654321;
        when(esiGateway.sendCharacterMail(eq(CHARACTER_ID), any(SendMailCommand.class), eq(STUB_ACCESS_TOKEN)))
                .thenReturn(newMailId);

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        // SendMailRequest DTO 由 T019 创建;此处构造假设与 SendMailCommand 对齐
        SendMailRequest request = new SendMailRequest(
                List.of(new SendMailRequest.RecipientItem(20001, "character")),
                "Test Subject",
                "Test body content"
        );

        Integer result = service.send(CHARACTER_ID, request);

        assertThat(result).isEqualTo(newMailId);
        verify(esiGateway).sendCharacterMail(eq(CHARACTER_ID), any(SendMailCommand.class), eq(STUB_ACCESS_TOKEN));
    }

    @Test
    @DisplayName("send 权限不足时抛出含「重新授权」提示的 EsiAccessException(US2-4)")
    void send_permissionLow_throwsReauthHint() throws ParseException {
        stubAuthorize();
        stubAccessToken();

        when(esiGateway.sendCharacterMail(eq(CHARACTER_ID), any(SendMailCommand.class), eq(STUB_ACCESS_TOKEN)))
                .thenThrow(EsiAccessException.permissionLow());

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        SendMailRequest request = new SendMailRequest(
                List.of(new SendMailRequest.RecipientItem(20001, "character")),
                "Subject",
                "Body"
        );

        // 服务原样透传 EsiAccessException,不包装新错误码
        assertThatThrownBy(() -> service.send(CHARACTER_ID, request))
                .isInstanceOf(EsiAccessException.class)
                .hasMessage(EsiAccessException.MSG_PERMISSION_LOW);
    }

    @Test
    @DisplayName("send 其他用户角色被拒绝(AuthorizeUtil 抛 ACCESS_UNAUTHORIZED)(US2-5)")
    void send_otherUsersCharacter_rejected() throws ParseException {
        when(authorizeUtil.authorize(CHARACTER_ID))
                .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        SendMailRequest request = new SendMailRequest(
                List.of(new SendMailRequest.RecipientItem(20001, "character")),
                "Subject",
                "Body"
        );

        assertThatThrownBy(() -> service.send(CHARACTER_ID, request))
                .isInstanceOf(EveHelperException.class)
                .satisfies(ex -> assertThat(((EveHelperException) ex).getResultCode())
                        .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));

        // 网关零调用:越权拒绝发生在取数之前
        verify(esiGateway, never()).getAccessTokenWithExpiry(anyInt(), anyInt());
        verify(esiGateway, never()).sendCharacterMail(anyInt(), any(), any());
    }

    /* ============================ US2 名称解析(公开,无授权) ============================ */

    @Test
    @DisplayName("resolveNames 部分未解析的名称标记 resolved=false(US2-7)")
    void resolveNames_partialUnresolved_marksResolvedFalse() {
        List<String> names = List.of("KnownChar", "UnknownName");

        // 网关返回全量结果:KnownChar 已解析,UnknownName 未解析
        when(esiGateway.resolveUniverseIds(names))
                .thenReturn(List.of(
                        new RecipientResolution("KnownChar", 20001, "character", true),
                        new RecipientResolution("UnknownName", null, null, false)
                ));

        MailApplicationService service = new MailApplicationService(authorizeUtil, esiGateway);

        // resolveNames 公开:无 authorize、无 token
        List<RecipientResolution> results = service.resolveNames(names);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).resolved()).isTrue();
        assertThat(results.get(0).id()).isEqualTo(20001);
        assertThat(results.get(1).resolved()).isFalse();
        assertThat(results.get(1).id()).isNull();

        // 网关只调用一次,无授权调用
        verify(esiGateway).resolveUniverseIds(names);
        verify(authorizeUtil, never()).authorize(anyInt());
    }
}
