package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.foolcat.eve.evehelper.application.service.MailApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailRecipient;
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
import xyz.foolcat.eve.evehelper.interfaces.web.advice.GlobalExceptionHandler;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US2 邮件控制器失败测试(RED-only, T015)。
 * <p>
 * 六个用例固化六端点行为:
 * GET /mail/{characterId}/inbox | GET /mail/{characterId}/labels |
 * GET /mail/{characterId}/{mailId} | PUT /mail/{characterId}/{mailId}/read |
 * POST /mail/{characterId}/send | POST /mail/recipients/resolve。
 * <p>
 * RED 原因:MailController 不存在,编译失败即 RED 证据。生产代码由 T020 创建。
 * 校验失败断言:MethodArgumentNotValidException 在 GlobalExceptionHandler 中无专属 handler,
 * Spring Boot 3 默认映射为 HTTP 400(状态码断言);不臆造 body 结构。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US2 邮件控制器测试")
class MailControllerTest {

    @Mock
    private MailApplicationService mailService;

    private MockMvc mockMvc;

    private static final Integer CHARACTER_ID = 95465499;
    private static final Integer MAIL_ID = 123456;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MailController(mailService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------
    //  inbox — cursor 可选
    // ---------------------------------------------------------------

    @Test
    @DisplayName("inbox lastMailId 游标参数可选(缺省不传)")
    void inbox_cursorParamOptional() throws Exception {
        // Arrange — 首页:不传 lastMailId
        MailHeaderPage stubPage = new MailHeaderPage(
                List.of(new MailHeaderPage.MailHeaderItem(
                        MAIL_ID, 10001, "First Mail",
                        OffsetDateTime.parse("2026-11-12T10:00:00Z"), false, List.of(1))),
                MAIL_ID
        );
        when(mailService.inbox(CHARACTER_ID, null, false)).thenReturn(stubPage);

        // Act & Assert — contracts §2 GET /mail/{characterId}/inbox (无 lastMailId 参数)
        mockMvc.perform(get("/mail/{characterId}/inbox", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.mails").isArray())
                .andExpect(jsonPath("$.data.mails[0].mailId").value(MAIL_ID))
                .andExpect(jsonPath("$.data.nextLastMailId").value(MAIL_ID));

        verify(mailService).inbox(CHARACTER_ID, null, false);
    }

    // ---------------------------------------------------------------
    //  send — 校验
    // ---------------------------------------------------------------

    @Test
    @DisplayName("send 校验拒绝超长 subject(>128 字符)")
    void send_validation_rejectsOversizedSubject() throws Exception {
        // Arrange — subject 129 字符(超上限 128)
        String oversizedSubject = IntStream.rangeClosed(1, 129)
                .mapToObj(i -> "A")
                .collect(Collectors.joining());

        String jsonBody = """
                {
                  "recipients": [{"recipientId": 20001, "recipientType": "character"}],
                  "subject": "%s",
                  "body": "Valid body"
                }
                """.formatted(oversizedSubject);

        // Act & Assert — @Size(max=128) 触发 MethodArgumentNotValidException → 400
        mockMvc.perform(post("/mail/{characterId}/send", CHARACTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("send 校验拒绝空收件人列表")
    void send_validation_rejectsEmptyRecipients() throws Exception {
        // Arrange — recipients 为空数组(@NotEmpty 违反)
        String jsonBody = """
                {
                  "recipients": [],
                  "subject": "Valid Subject",
                  "body": "Valid body"
                }
                """;

        // Act & Assert — @NotEmpty 触发 MethodArgumentNotValidException → 400
        mockMvc.perform(post("/mail/{characterId}/send", CHARACTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("send 校验拒绝 null recipientType(MIN-4:@Pattern 对 null 放行,须 @NotNull 兜底)")
    void send_validation_rejectsNullRecipientType() throws Exception {
        // Arrange — recipientType 缺省(null):@Pattern 不校验 null,
        // 若无 @NotNull 会穿透到 ESI 才 400(浪费外部调用且错误语义滞后)
        String jsonBody = """
                {
                  "recipients": [{"recipientId": 20001}],
                  "subject": "Valid Subject",
                  "body": "Valid body"
                }
                """;

        // Act & Assert — @NotNull 触发校验失败 → 400 + PARAM_ERROR,不触达应用服务/ESI
        mockMvc.perform(post("/mail/{characterId}/send", CHARACTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SYS00400"));

        verify(mailService, never()).send(any(), any());
    }

    // ---------------------------------------------------------------
    //  resolve — 校验
    // ---------------------------------------------------------------

    @Test
    @DisplayName("resolve 校验拒绝超过 50 个名称")
    void resolve_validation_rejectsMoreThan50Names() throws Exception {
        // Arrange — 51 个名称(超上限 50)
        String namesJson = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "\"Name" + i + "\"")
                .collect(Collectors.joining(","));

        String jsonBody = """
                {
                  "names": [%s]
                }
                """.formatted(namesJson);

        // Act & Assert — @Size(max=50) 触发 MethodArgumentNotValidException → 400
        mockMvc.perform(post("/mail/recipients/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    //  markRead — 必填校验
    // ---------------------------------------------------------------

    @Test
    @DisplayName("markRead 要求 read 字段非 null")
    void markRead_requiresNonNullRead() throws Exception {
        // Arrange — body 中省略 read 字段(@NotNull Boolean read 违反)
        String jsonBody = """
                {
                }
                """;

        // Act & Assert — @NotNull 触发 MethodArgumentNotValidException → 400
        mockMvc.perform(put("/mail/{characterId}/{mailId}/read", CHARACTER_ID, MAIL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    //  labels — 成功响应
    // ---------------------------------------------------------------

    @Test
    @DisplayName("labels 成功响应包含标签列表与总未读数")
    void labels_response_containsLabelsAndTotalUnread() throws Exception {
        // Arrange
        MailLabelsInfo stubInfo = new MailLabelsInfo(
                List.of(new MailLabelsInfo.MailLabel(1, "Inbox", 5L)),
                5L
        );
        when(mailService.labels(CHARACTER_ID, false)).thenReturn(stubInfo);

        // Act & Assert
        mockMvc.perform(get("/mail/{characterId}/labels", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.labels[0].labelId").value(1))
                .andExpect(jsonPath("$.data.totalUnread").value(5));

        verify(mailService).labels(CHARACTER_ID, false);
    }

    // ---------------------------------------------------------------
    //  detail — 成功响应
    // ---------------------------------------------------------------

    @Test
    @DisplayName("detail 成功响应包含邮件正文")
    void detail_response_containsMailBody() throws Exception {
        // Arrange
        MailDetailInfo stubDetail = new MailDetailInfo(
                MAIL_ID, 10001,
                List.of(new MailRecipient(CHARACTER_ID, "character")),
                "Test Subject",
                OffsetDateTime.parse("2026-11-12T10:00:00Z"),
                "<p>Hello World</p>",
                false
        );
        when(mailService.detail(CHARACTER_ID, MAIL_ID, false)).thenReturn(stubDetail);

        // Act & Assert
        mockMvc.perform(get("/mail/{characterId}/{mailId}", CHARACTER_ID, MAIL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.mailId").value(MAIL_ID))
                .andExpect(jsonPath("$.data.subject").value("Test Subject"))
                .andExpect(jsonPath("$.data.body").value("<p>Hello World</p>"));

        verify(mailService).detail(CHARACTER_ID, MAIL_ID, false);
    }

    // ---------------------------------------------------------------
    //  send — 成功响应
    // ---------------------------------------------------------------

    @Test
    @DisplayName("send 成功响应包含 mailId")
    void send_response_containsMailId() throws Exception {
        // Arrange
        Integer newMailId = 654321;
        when(mailService.send(eq(CHARACTER_ID), any())).thenReturn(newMailId);

        String jsonBody = """
                {
                  "recipients": [{"recipientId": 20001, "recipientType": "character"}],
                  "subject": "Hello",
                  "body": "Test message body"
                }
                """;

        // Act & Assert — contracts §2 POST /mail/{characterId}/send 响应: {mailId}
        mockMvc.perform(post("/mail/{characterId}/send", CHARACTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.mailId").value(newMailId));
    }
}
