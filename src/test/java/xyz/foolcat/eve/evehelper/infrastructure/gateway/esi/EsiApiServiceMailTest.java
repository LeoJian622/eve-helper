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
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Name2IdResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.RequestedMailResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.NewMail;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.IdName;
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
    @DisplayName("send SDK 异步 EsiException(permissionLow) → 转换为 EsiAccessException(IMP-1)")
    void send_sdkError_convertedToAccessException() {
        // Arrange: SDK 真实错误路径是异步 Mono.error(EsiStatusUtil.dataError()),
        // 非同步 throw —— 异步 stub 钉住生产可达分支(IMP-1)
        SendMailCommand command = new SendMailCommand(
                List.of(new MailRecipient(12345, "character")),
                "Subject", "Body");
        when(mailApi.addCharacterMail(eq(CHARACTER_ID), eq(DATASOURCE), any(NewMail.class), eq(ACCESS_TOKEN)))
                .thenReturn(Mono.error(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW)));

        // Act & Assert
        assertThatThrownBy(() -> esiApiService.sendCharacterMail(CHARACTER_ID, command, ACCESS_TOKEN))
                .isInstanceOf(EsiAccessException.class)
                .hasMessageContaining(EsiAccessException.MSG_PERMISSION_LOW);
    }

    // ── T018: universe/ids 精确名称解析 ──

    @Test
    @DisplayName("resolve 使用 pub 缓存段(userId=null),不查 UserUtil")
    void resolve_pubCacheSegment_noUserContext() {
        // Arrange: pub 段,缓存未命中,SDK 返回解析结果
        List<String> names = List.of("TestChar");
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/TestChar/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/TestChar/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        Name2IdResponse sdkBody = buildName2IdResponse(List.of(new IdNameEntry("TestChar", 12345, "character")));
        when(universeApi.queryUniverseIdsResponse(eq(names), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act
        List<RecipientResolution> result = esiApiService.resolveUniverseIds(names);

        // Assert: pub 段(userId=null)被使用,UserUtil 未被查询
        verify(esiResponseCache).cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/TestChar/"), eq(0));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("TestChar");
        assertThat(result.get(0).id()).isEqualTo(12345);
        assertThat(result.get(0).type()).isEqualTo("character");
        assertThat(result.get(0).resolved()).isTrue();
    }

    @Test
    @DisplayName("resolve 排序 names → 确定性缓存键(输入顺序无关)")
    void resolve_sortedNames_deterministicCacheKey() {
        // Arrange: 两个不同顺序的 names 列表应生成相同缓存键
        List<String> namesAB = List.of("Bravo", "Alpha");
        List<String> namesBA = List.of("Alpha", "Bravo");
        // 排序后均为 "Alpha|Bravo",缓存键路径段 = "/universe/ids/Alpha|Bravo/"
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/Alpha|Bravo/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha|Bravo/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        Name2IdResponse sdkBody = buildName2IdResponse(List.of(
                new IdNameEntry("Alpha", 100, "character"),
                new IdNameEntry("Bravo", 200, "corporation")
        ));
        when(universeApi.queryUniverseIdsResponse(any(), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act: 两种输入顺序
        esiApiService.resolveUniverseIds(namesAB);
        esiApiService.resolveUniverseIds(namesBA);

        // Assert: 缓存键生成均使用排序后的路径段
        verify(esiResponseCache, atLeast(2)).cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha|Bravo/"), eq(0));
    }

    @Test
    @DisplayName("resolve 名称含连字符不碰撞（P2 encodeNames 分隔符修复）")
    void resolve_namesWithHyphen_noCollision() {
        // Arrange: 两组不同名称列表,排序后 joining("-") 会碰撞
        //   组 A: ["Alpha-Beta", "Gamma"] → sorted: ["Alpha-Beta", "Gamma"]
        //   组 B: ["Alpha", "Beta-Gamma"] → sorted: ["Alpha", "Beta-Gamma"]
        // 用 "|" 分隔后: "Alpha-Beta|Gamma" ≠ "Alpha|Beta-Gamma"
        List<String> groupA = List.of("Alpha-Beta", "Gamma");
        List<String> groupB = List.of("Alpha", "Beta-Gamma");

        String keyA = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/Alpha-Beta|Gamma/:0";
        String keyB = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/Alpha|Beta-Gamma/:0";

        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha-Beta|Gamma/"), eq(0)))
                .thenReturn(keyA);
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha|Beta-Gamma/"), eq(0)))
                .thenReturn(keyB);
        when(esiResponseCache.getList(anyString(), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(anyString(), any())).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        when(universeApi.queryUniverseIdsResponse(any(), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(buildName2IdResponse(List.of()))));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act
        esiApiService.resolveUniverseIds(groupA);
        esiApiService.resolveUniverseIds(groupB);

        // Assert: 两组使用不同缓存键(不碰撞)
        verify(esiResponseCache).cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha-Beta|Gamma/"), eq(0));
        verify(esiResponseCache).cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Alpha|Beta-Gamma/"), eq(0));
    }

    @Test
    @DisplayName("resolve 混合类型(characters/corporations/alliances)全映射")
    void resolve_mixedTypes_mapsAllGroups() {
        // Arrange: SDK 返回三种类型
        List<String> names = List.of("CharA", "CorpB", "AllyC");
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/AllyC|CharA|CorpB/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/AllyC|CharA|CorpB/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        Name2IdResponse sdkBody = new Name2IdResponse();
        IdName charA = new IdName(); charA.setId(100); charA.setName("CharA");
        IdName corpB = new IdName(); corpB.setId(200); corpB.setName("CorpB");
        IdName allyC = new IdName(); allyC.setId(300); allyC.setName("AllyC");
        sdkBody.setCharacters(List.of(charA));
        sdkBody.setCorporations(List.of(corpB));
        sdkBody.setAlliances(List.of(allyC));
        when(universeApi.queryUniverseIdsResponse(eq(names), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act
        List<RecipientResolution> result = esiApiService.resolveUniverseIds(names);

        // Assert: 三种类型均被映射
        assertThat(result).hasSize(3);
        RecipientResolution charRes = result.stream().filter(r -> r.name().equals("CharA")).findFirst().orElseThrow();
        assertThat(charRes.id()).isEqualTo(100);
        assertThat(charRes.type()).isEqualTo("character");
        assertThat(charRes.resolved()).isTrue();

        RecipientResolution corpRes = result.stream().filter(r -> r.name().equals("CorpB")).findFirst().orElseThrow();
        assertThat(corpRes.id()).isEqualTo(200);
        assertThat(corpRes.type()).isEqualTo("corporation");
        assertThat(corpRes.resolved()).isTrue();

        RecipientResolution allyRes = result.stream().filter(r -> r.name().equals("AllyC")).findFirst().orElseThrow();
        assertThat(allyRes.id()).isEqualTo(300);
        assertThat(allyRes.type()).isEqualTo("alliance");
        assertThat(allyRes.resolved()).isTrue();
    }

    @Test
    @DisplayName("resolve 未命中名称 → resolved=false 条目(保持请求名全量返回)")
    void resolve_unknownName_resolvedFalse() {
        // Arrange: 请求 2 个名称,SDK 仅命中 1 个
        List<String> names = List.of("Known", "Unknown");
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/Known|Unknown/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/Known|Unknown/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        Name2IdResponse sdkBody = buildName2IdResponse(List.of(new IdNameEntry("Known", 999, "character")));
        when(universeApi.queryUniverseIdsResponse(eq(names), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act
        List<RecipientResolution> result = esiApiService.resolveUniverseIds(names);

        // Assert: 2 条,Known 已解析,Unknown resolved=false
        assertThat(result).hasSize(2);
        RecipientResolution known = result.stream().filter(r -> r.name().equals("Known")).findFirst().orElseThrow();
        assertThat(known.resolved()).isTrue();
        assertThat(known.id()).isEqualTo(999);

        RecipientResolution unknown = result.stream().filter(r -> r.name().equals("Unknown")).findFirst().orElseThrow();
        assertThat(unknown.resolved()).isFalse();
        assertThat(unknown.id()).isNull();
        assertThat(unknown.type()).isNull();
    }

    @Test
    @DisplayName("resolve 缓存写使用响应头提取的 TTL(非 null)")
    void resolve_cacheWrite_usesHeaderTtl() {
        // Arrange
        List<String> names = List.of("TestName");
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/universe/ids/TestName/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/universe/ids/TestName/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        // extractor 被 mock,头设置仅为可读性
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
        Name2IdResponse sdkBody = buildName2IdResponse(List.of(new IdNameEntry("TestName", 555, "character")));
        when(universeApi.queryUniverseIdsResponse(eq(names), eq(DATASOURCE), isNull()))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
        when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

        // Act
        esiApiService.resolveUniverseIds(names);

        // Assert: put 收到非 null TTL 且等于从响应头提取的值
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(esiResponseCache).put(eq(expectedKey), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isNotNull();
        assertThat(ttlCaptor.getValue()).isEqualTo(EXPECTED_TTL);
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

    private record IdNameEntry(String name, Integer id, String type) {}

    private Name2IdResponse buildName2IdResponse(List<IdNameEntry> entries) {
        Name2IdResponse resp = new Name2IdResponse();
        List<IdName> characters = entries.stream().filter(e -> "character".equals(e.type))
                .map(e -> { IdName n = new IdName(); n.setId(e.id); n.setName(e.name); return n; }).toList();
        List<IdName> corporations = entries.stream().filter(e -> "corporation".equals(e.type))
                .map(e -> { IdName n = new IdName(); n.setId(e.id); n.setName(e.name); return n; }).toList();
        List<IdName> alliances = entries.stream().filter(e -> "alliance".equals(e.type))
                .map(e -> { IdName n = new IdName(); n.setId(e.id); n.setName(e.name); return n; }).toList();
        resp.setCharacters(characters.isEmpty() ? null : characters);
        resp.setCorporations(corporations.isEmpty() ? null : corporations);
        resp.setAlliances(alliances.isEmpty() ? null : alliances);
        return resp;
    }
}
