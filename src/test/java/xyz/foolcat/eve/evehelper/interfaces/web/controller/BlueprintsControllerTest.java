package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;



@SpringBootTest
@ActiveProfiles("test")
@DisplayName("蓝图接口测试")
@AutoConfigureMockMvc
@WithUserDetails("admin")
class BlueprintsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void addBlueprintsList() throws Exception {
        // 修复：去掉错误的 /1/ 前缀与 /char/ 段，命中 GET /blueprints/{id}
        String url = "/blueprints/2112832425";
        String result = mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        System.out.println(result);
    }

    @Test
    void getBlueprintsList() throws Exception {
        // 修复：去掉错误的 /1/ 前缀，命中 GET /blueprints/{id}
        String url = "/blueprints/2112832425";
        String result = mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        System.out.println(result);
    }
}