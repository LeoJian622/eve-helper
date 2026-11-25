package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import xyz.foolcat.eve.evehelper.application.service.CharacterApplicationService;
import xyz.foolcat.eve.evehelper.application.service.UserApplicationService;
import xyz.foolcat.eve.evehelper.interfaces.web.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 改密控制器单元测试（T041，2 用例）。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("改密控制器测试(T041)")
class UserControllerPasswordTest {

    @Mock
    private UserApplicationService userApplicationService;

    @Mock
    private CharacterApplicationService characterApplicationService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userApplicationService, characterApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ── AC: 正确请求委托 service ──

    @Test
    @DisplayName("putPassword_delegatesToService: 200 + $.code==200 + verify changePassword")
    void putPassword_delegatesToService() throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<String, String>() {{
                    put("oldPassword", "OldPass123!");
                    put("newPassword", "NewPass456!");
                }}
        );

        mockMvc.perform(put("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(userApplicationService).changePassword(any());
    }

    // ── AC: 短密码被校验拒绝 ──

    @Test
    @DisplayName("putPassword_validation_rejectsShortNewPassword: 400 + never")
    void putPassword_validation_rejectsShortNewPassword() throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<String, String>() {{
                    put("oldPassword", "OldPass123!");
                    put("newPassword", "short");
                }}
        );

        mockMvc.perform(put("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(userApplicationService, never()).changePassword(any());
    }
}
