package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

/**
 * 建筑接口测试(US1~US6 冒烟)
 * admin(ROOT)豁免军团归属校验;详情/服务测试动态从列表接口取真实 structureId,
 * 避免硬编码测试库可能不存在的建筑 ID
 *
 * @author Leojan
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("建筑接口测试")
@AutoConfigureMockMvc
@WithUserDetails("admin")
class StructureControllerTest {

    @Autowired
    MockMvc mockMvc;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("列表-响应体结构断言(code/data/records/分页)")
    void getStructuresList() throws Exception {
        String url = "/structures/98000001?current=1&size=20&sortField=fuelExpires&sortOrder=asc";
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.records").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.current").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").isNumber())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("建筑详情-响应体结构断言(code/data/structureId)")
    void getStructureDetail() throws Exception {
        Long structureId = firstStructureIdOf("98000001");
        Assumptions.assumeTrue(structureId != null, "测试库 98000001 军团无建筑,跳过详情冒烟");
        String url = "/structures/98000001/detail/" + structureId;
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.structureId").value(structureId))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("燃料预警-响应体结构断言(code/data 数组)")
    void getStructureFuel() throws Exception {
        String url = "/structures/98000001/fuel?hours=48";
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("建筑服务状态-响应体结构断言(code/data/structureId/services 数组)")
    void getStructureServices() throws Exception {
        Long structureId = firstStructureIdOf("98000001");
        Assumptions.assumeTrue(structureId != null, "测试库 98000001 军团无建筑,跳过服务状态冒烟");
        String url = "/structures/98000001/detail/" + structureId + "/services";
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.structureId").value(structureId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.services").isArray())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("统计概览-响应体结构断言(code/data/total/stateCounts)")
    void getStructureStats() throws Exception {
        String url = "/structures/98000001/stats";
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.total").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.stateCounts").isMap())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("时间提醒-响应体结构断言(code/data 数组)")
    void getStructureTimers() throws Exception {
        String url = "/structures/98000001/timers";
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andDo(MockMvcResultHandlers.print());
    }

    /**
     * 从列表接口动态获取军团第一栋建筑 ID,避免硬编码测试库可能不存在的 structureId
     */
    private Long firstStructureIdOf(String corpId) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.get("/structures/" + corpId + "?current=1&size=1"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode records = root.path("data").path("records");
            if (records.isArray() && records.size() > 0) {
                long id = records.get(0).path("structureId").asLong();
                return id > 0 ? id : null;
            }
        } catch (Exception e) {
            // 解析失败视为无可用建筑
        }
        return null;
    }
}
