package xyz.foolcat.eve.evehelper.infrastructure.gateway.esi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailRecipient;
import xyz.foolcat.eve.evehelper.domain.model.vo.SendMailCommand;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiCacheTtlExtractor;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.MailLabelsAndUnreadCountsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.MailResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.RequestedMailResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.NewMail;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Label;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 邮件网关单元测试(T016+T017+T018)。
 * <p>
 * 纯单元测试,不依赖 Spring 上下文;mock SDK API、缓存组件与限流器,
 * 验证邮件读侧/写侧/universe 名称解析的缓存段归属、游标语义、异常转换与 TTL 写入。
 *
 * @author Leojan
 * date 2026-11-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("邮件网关(T016+T017+T018)")
class EsiApiServiceMailTest {

    @Mock private CacheGateway cacheGateway;
    @Mock private EveAccountService eveAccountService;
    @Mock private AuthorizeOAuth authorizeOAuth;
    @Mock private CharacterApi characterApi;
    @Mock private UniverseApi universeApi;
    @Mock private AssetsApi assetsApi;
    @Mock private CorporationApi corporationApi;
    @Mock private IndustryApi industryApi;
    @Mock private WalletApi walletApi;
    @Mock private LocationApi locationApi;
    @Mock private SkillApi skillApi;
    @Mock private MailApi mailApi;
    @Mock private EsiAssetsConverter esiAssetsConverter;
    @Mock private EsiIndustryJobConverter esiIndustryJobConverter;
    @Mock private EsiInvTypesConverter esiInvTypesConverter;
    @Mock private EsiMiningDetailConverter esiMiningDetailConverter;
    @Mock private EsiStructureConverter esiStructureConverter;
    @Mock private EsiUniverseNameConverter esiUniverseNameConverter;
    @Mock private EsiWalletJournalConverter esiWalletJournalConverter;
    @Mock private EsiWalletTransactionConverter esiWalletTransactionConverter;
    @Mock private EsiResponseCache esiResponseCache;
    @Mock private EsiCacheTtlExtractor esiCacheTtlExtractor;
    @Mock private ForceRefreshLimiter forceRefreshLimiter;
    @Mock private EsiResponseCacheProperties esiResponseCacheProperties;

    private EsiApiService esiApiService;

    private static final Integer CHARACTER_ID = 95465499;
    private static final Integer USER_ID = 100;
    private static final String ACCESS_TOKEN = "Bearer test-token";
    private static final String DATASOURCE = "serenity";
    private static final Duration EXPECTED_TTL = Duration.ofSeconds(300);

    @BeforeEach
    void setUp() {
        esiApiService = new EsiApiService(
                cacheGateway, eveAccountService, authorizeOAuth,
                characterApi, universeApi, assetsApi, corporationApi, industryApi, walletApi, locationApi, skillApi, mailApi,
                esiAssetsConverter, esiIndustryJobConverter, esiInvTypesConverter,
                esiMiningDetailConverter, esiStructureConverter,
                esiUniverseNameConverter, esiWalletJournalConverter, esiWalletTransactionConverter,
                esiResponseCache, esiCacheTtlExtractor, forceRefreshLimiter,
                esiResponseCacheProperties, new ObjectMapper()
        );
        lenient().when(esiResponseCacheProperties.getDatasource()).thenReturn(DATASOURCE);
        lenient().when(esiResponseCacheProperties.getForceRefreshWindowMinutes()).thenReturn(60);
        lenient().when(esiResponseCacheProperties.getForceRefreshLimit()).thenReturn(5);
    }

    // ── T016: 邮件 headers ──

    @Test
    @DisplayName("headers 满 50 条 → nextLastMailId = 本页最后一条 mailId")
    void headers_nextLastMailId_equalsLastMailId_whenFull50() {
        // Arrange: 缓存未命中,SDK 返回 50 条邮件
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            List<RequestedMailResponse> sdkBody = buildMailHeaders(50);
            when(mailApi.queryCharacterMailsResponse(eq(CHARACTER_ID), eq(DATASOURCE), isNull(), isNull(), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            MailHeaderPage result = esiApiService.queryCharacterMailHeaders(CHARACTER_ID, null, ACCESS_TOKEN);

            // Assert: 满 50 条 → nextLastMailId = 最后一条 mailId
            assertThat(result).isNotNull();
            assertThat(result.mails()).hasSize(50);
            assertThat(result.nextLastMailId()).isEqualTo(1001);
        }
    }

    @Test
    @DisplayName("headers 不足 50 条 → nextLastMailId = null(无下一页)")
    void headers_nextLastMailId_null_whenLessThan50() {
        // Arrange: 缓存未命中,SDK 返回 30 条邮件
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            List<RequestedMailResponse> sdkBody = buildMailHeaders(30);
            when(mailApi.queryCharacterMailsResponse(eq(CHARACTER_ID), eq(DATASOURCE), isNull(), isNull(), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            MailHeaderPage result = esiApiService.queryCharacterMailHeaders(CHARACTER_ID, null, ACCESS_TOKEN);

            // Assert: 不足 50 条 → nextLastMailId = null
            assertThat(result).isNotNull();
            assertThat(result.mails()).hasSize(30);
            assertThat(result.nextLastMailId()).isNull();
        }
    }

    @Test
    @DisplayName("labels 映射 totalUnreadCount")
    void labels_mapsTotalUnreadCount() {
        // Arrange: 缓存未命中,SDK 返回标签列表
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/labels/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/labels/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            MailLabelsAndUnreadCountsResponse sdkBody = buildMailLabelsResponse();
            when(mailApi.queryCharacterMailLabelsResponse(eq(CHARACTER_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            MailLabelsInfo result = esiApiService.queryCharacterMailLabels(CHARACTER_ID, ACCESS_TOKEN);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.totalUnread()).isEqualTo(42L);
            assertThat(result.labels()).hasSize(2);
            assertThat(result.labels().get(0).labelId()).isEqualTo(1);
            assertThat(result.labels().get(0).name()).isEqualTo("Inbox");
            assertThat(result.labels().get(0).unreadCount()).isEqualTo(10L);
        }
    }

    @Test
    @DisplayName("body 返回原始 HTML 内容")
    void body_returnsRawContent() {
        // Arrange: 缓存未命中,SDK 返回邮件详情(含 HTML body)
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            Integer mailId = 5001;
            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/" + mailId + "/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/" + mailId + "/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            MailResponse sdkBody = buildMailDetailResponse();
            when(mailApi.queryCharacterMailResponse(eq(CHARACTER_ID), eq(DATASOURCE), eq(mailId), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            MailDetailInfo result = esiApiService.queryCharacterMail(CHARACTER_ID, mailId, ACCESS_TOKEN);

            // Assert: body 原样返回
            assertThat(result).isNotNull();
            assertThat(result.body()).isEqualTo("<p>Test mail body</p>");
            assertThat(result.subject()).isEqualTo("Test Subject");
            assertThat(result.from()).isEqualTo(12345);
        }
    }

    @Test
    @DisplayName("headers 缓存命中 → 直接返回,不调用 SDK")
    void headers_cacheHit_noSdkCall() {
        // Arrange: 缓存命中
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null"), eq(0)))
                    .thenReturn(expectedKey);

            MailHeaderPage cachedPage = new MailHeaderPage(
                    List.of(new MailHeaderPage.MailHeaderItem(9999, 12345, "Cached", OffsetDateTime.now(), true, List.of())),
                    null
            );
            when(esiResponseCache.getList(eq(expectedKey), any()))
                    .thenReturn(List.of(cachedPage));

            // Act
            MailHeaderPage result = esiApiService.queryCharacterMailHeaders(CHARACTER_ID, null, ACCESS_TOKEN);

            // Assert: 缓存命中,SDK 未被调用
            assertThat(result).isNotNull();
            assertThat(result.mails()).hasSize(1);
            assertThat(result.mails().get(0).mailId()).isEqualTo(9999);
            verify(mailApi, never()).queryCharacterMailsResponse(anyInt(), anyString(), any(), any(), anyString());
        }
    }

    @Test
    @DisplayName("headers 缓存写使用响应头提取的 TTL(非 null)")
    void headers_cacheWrite_usesHeaderTtl() {
        // Arrange
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/mail/?last_mail_id=null"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            // extractor 被 mock,头设置仅为可读性
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            List<RequestedMailResponse> sdkBody = buildMailHeaders(10);
            when(mailApi.queryCharacterMailsResponse(eq(CHARACTER_ID), eq(DATASOURCE), isNull(), isNull(), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            esiApiService.queryCharacterMailHeaders(CHARACTER_ID, null, ACCESS_TOKEN);

            // Assert: put 收到非 null TTL 且等于从响应头提取的值
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(esiResponseCache).put(eq(expectedKey), any(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isNotNull();
            assertThat(ttlCaptor.getValue()).isEqualTo(EXPECTED_TTL);
        }
    }

    // ── T017: 邮件写侧 ──

    @Test
    @DisplayName("markRead 委托 read 标记到 SDK updateCharacterMail")
    void markRead_delegatesReadFlag() {
        // Arrange: SDK updateCharacterMail 返回成功
        Integer mailId = 5001;
        when(mailApi.updateCharacterMail(eq(CHARACTER_ID), eq(DATASOURCE), eq(mailId), isNull(), eq(true), eq(ACCESS_TOKEN)))
                .thenReturn(Mono.just(new MailResponse()));

        // Act
        esiApiService.markCharacterMailRead(CHARACTER_ID, mailId, true, ACCESS_TOKEN);

        // Assert: SDK 被调用,read=true 传递
        verify(mailApi).updateCharacterMail(CHARACTER_ID, DATASOURCE, mailId, null, true, ACCESS_TOKEN);
    }

    @Test
    @DisplayName("send 映射 recipients 并返回 mailId")
    void send_mapsRecipientsAndReturnsMailId() {
        // Arrange: SDK addCharacterMail 返回 mailId
        Integer expectedMailId = 9001;
        List<MailRecipient> recipients = List.of(
                new MailRecipient(12345, "character"),
                new MailRecipient(67890, "corporation")
        );
        SendMailCommand command = new SendMailCommand(recipients, "Test Subject", "<p>Body</p>");

        when(mailApi.addCharacterMail(eq(CHARACTER_ID), eq(DATASOURCE), any(NewMail.class), eq(ACCESS_TOKEN)))
                .thenReturn(Mono.just(expectedMailId));

        // Act
        Integer result = esiApiService.sendCharacterMail(CHARACTER_ID, command, ACCESS_TOKEN);

        // Assert: 返回 mailId,SDK 被调用且 NewMail 映射正确
        assertThat(result).isEqualTo(expectedMailId);
        ArgumentCaptor<NewMail> captor = ArgumentCaptor.forClass(NewMail.class);
        verify(mailApi).addCharacterMail(eq(CHARACTER_ID), eq(DATASOURCE), captor.capture(), eq(ACCESS_TOKEN));
        NewMail sentMail = captor.getValue();
        assertThat(sentMail.getSubject()).isEqualTo("Test Subject");
        assertThat(sentMail.getBody()).isEqualTo("<p>Body</p>");
        assertThat(sentMail.getRecipients()).hasSize(2);
        assertThat(sentMail.getRecipients().get(0).getRecipientId()).isEqualTo(12345);
        assertThat(sentMail.getRecipients().get(0).getRecipientType()).isEqualTo("character");
        assertThat(sentMail.getRecipients().get(1).getRecipientId()).isEqualTo(67890);
        assertThat(sentMail.getRecipients().get(1).getRecipientType()).isEqualTo("corporation");
    }

    @Test
    @DisplayName("send SDK EsiException(permissionLow) → 转换为 EsiAccessException")
    void send_sdkError_convertedToAccessException() {
        // Arrange: SDK 抛出 EsiException(403 权限不足)
        SendMailCommand command = new SendMailCommand(
                List.of(new MailRecipient(12345, "character")),
                "Subject", "Body");
        when(mailApi.addCharacterMail(eq(CHARACTER_ID), eq(DATASOURCE), any(NewMail.class), eq(ACCESS_TOKEN)))
                .thenThrow(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        // Act & Assert
        assertThatThrownBy(() -> esiApiService.sendCharacterMail(CHARACTER_ID, command, ACCESS_TOKEN))
                .isInstanceOf(EsiAccessException.class)
                .hasMessageContaining(EsiAccessException.MSG_PERMISSION_LOW);
    }

    // ── helpers ──

    private List<RequestedMailResponse> buildMailHeaders(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    RequestedMailResponse resp = new RequestedMailResponse();
                    resp.setMailId(1050 - i);
                    resp.setFrom(12345);
                    resp.setSubject("Mail " + i);
                    resp.setTimestamp(OffsetDateTime.parse("2026-11-12T10:00:00Z"));
                    resp.setIsRead(i % 2 == 0);
                    resp.setLabels(List.of(1, 2));
                    return resp;
                })
                .toList();
    }

    private MailLabelsAndUnreadCountsResponse buildMailLabelsResponse() {
        MailLabelsAndUnreadCountsResponse resp = new MailLabelsAndUnreadCountsResponse();
        resp.setTotalUnreadCount(42);

        Label l1 = new Label();
        l1.setLabelId(1);
        l1.setName("Inbox");
        l1.setUnreadCount(10);

        Label l2 = new Label();
        l2.setLabelId(2);
        l2.setName("Corp");
        l2.setUnreadCount(32);

        resp.setLabels(List.of(l1, l2));
        return resp;
    }

    private MailResponse buildMailDetailResponse() {
        MailResponse resp = new MailResponse();
        resp.setBody("<p>Test mail body</p>");
        resp.setFrom(12345);
        resp.setSubject("Test Subject");
        resp.setRead(true);
        resp.setTimestamp(OffsetDateTime.parse("2026-11-12T10:00:00Z"));
        resp.setLabels(List.of(1));

        Recipient r = new Recipient();
        r.setRecipientId(CHARACTER_ID);
        r.setRecipientType("character");
        resp.setRecipients(List.of(r));
        return resp;
    }
}
