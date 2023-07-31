package xyz.foolcat.eve.evehelper.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("资产接口测试")
@AutoConfigureMockMvc
@WithUserDetails("admin")
class AssetsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void addAssetsList() throws Exception {
        String url = "/1/asserts/char/2112832425";
        String result = mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        System.out.println(result);
    }

    @Test
    void getAssetsList() throws Exception {
        String url = "/1/assert/2112832425";
        String result = mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        System.out.println(result);
    }
}