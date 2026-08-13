package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;



@SpringBootTest
@ActiveProfiles("test")
@DisplayName("人物接口测试")
@AutoConfigureMockMvc
class CharacterControllerTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * 禁用：此测试用硬编码的一次性 SSO 授权码真实调 ESI 换 token，code 已失效返回 400，
     * 无法在测试环境稳定通过。已修正 URL（去掉错误 /crop/ 段命中 POST /character/{code}），
     * 需真实有效授权码时替换 code 后临时启用。
     */
    @Test
    @Disabled("需真实有效的一次性 SSO 授权码，硬编码 code 已失效")
    void addCharacterAuth() throws Exception {
        final String result = mockMvc.perform(MockMvcRequestBuilders.post(
                        "/character/GAUu5McvqEi40KY5ytn3CQ")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}