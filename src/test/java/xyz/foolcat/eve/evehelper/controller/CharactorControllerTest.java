package xyz.foolcat.eve.evehelper.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("角色接口测试")
@AutoConfigureMockMvc
class CharactorControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void addCharactorAuth() throws Exception {
        String url = "https://esi.evepc.163.com/ui/oauth2-redirect.html?code=2DlJ1amI3Uu6u-SxP_M_pA&state=T";
        final String result = mockMvc.perform(MockMvcRequestBuilders.post(
                        "/charactor/2DlJ1amI3Uu6u-SxP_M_pA")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}