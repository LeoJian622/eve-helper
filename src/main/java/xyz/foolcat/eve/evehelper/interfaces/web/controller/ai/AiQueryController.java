package xyz.foolcat.eve.evehelper.interfaces.web.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.ai.AiQueryRequest;
import xyz.foolcat.eve.evehelper.application.dto.ai.AiQueryResponse;
import xyz.foolcat.eve.evehelper.application.dto.ai.QueryHistoryResponse;
import xyz.foolcat.eve.evehelper.application.service.ai.AiQueryApplicationService;

import java.util.List;

/**
 * AI查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI查询", description = "自然语言数据库查询相关接口")
public class AiQueryController {

    private final AiQueryApplicationService aiQueryApplicationService;

    /**
     * 执行自然语言查询
     */
    @PostMapping("/query")
    @Operation(summary = "执行自然语言查询", description = "将用户的自然语言问题转换为SQL并执行查询")
    public ResponseEntity<AiQueryResponse> executeQuery(
            @Valid @RequestBody AiQueryRequest request,
            Authentication authentication) {

        // 从认证信息中获取用户ID（根据实际安全实现调整）
        Integer userId = getUserIdFromAuthentication(authentication);

        log.info("User {} executing AI query: {}", userId, request.getQuestion());

        AiQueryResponse response = aiQueryApplicationService.executeQuery(
                userId,
                request.getQuestion(),
                request.getSessionId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 获取查询历史列表
     */
    @GetMapping("/history")
    @Operation(summary = "获取查询历史", description = "获取当前用户的查询历史记录")
    public ResponseEntity<List<QueryHistoryResponse>> getQueryHistory(
            @RequestParam(defaultValue = "100") int limit,
            Authentication authentication) {

        Integer userId = getUserIdFromAuthentication(authentication);
        List<QueryHistoryResponse> history = aiQueryApplicationService.getQueryHistory(userId, limit);

        return ResponseEntity.ok(history);
    }

    /**
     * 获取单条查询详情
     */
    @GetMapping("/history/{id}")
    @Operation(summary = "获取查询详情", description = "获取单条查询历史记录的详细信息")
    public ResponseEntity<AiQueryResponse> getQueryDetail(@PathVariable Long id) {
        AiQueryResponse detail = aiQueryApplicationService.getQueryDetail(id);

        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }

    /**
     * 删除查询历史记录
     */
    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除查询历史", description = "删除指定的查询历史记录")
    public ResponseEntity<Void> deleteQueryHistory(@PathVariable Long id) {
        aiQueryApplicationService.deleteQueryHistory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 清空当前会话的AI记忆
     */
    @DeleteMapping("/memory/{sessionId}")
    @Operation(summary = "清空会话记忆", description = "清空指定会话的AI对话记忆")
    public ResponseEntity<Void> clearSessionMemory(@PathVariable String sessionId) {
        aiQueryApplicationService.clearSessionMemory(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前会话的历史消息数
     */
    @GetMapping("/memory/{sessionId}/count")
    @Operation(summary = "获取会话历史消息数", description = "获取指定会话的对话历史消息数量")
    public ResponseEntity<Integer> getSessionHistoryCount(@PathVariable String sessionId) {
        int count = aiQueryApplicationService.getSessionHistoryCount(sessionId);
        return ResponseEntity.ok(count);
    }

    /**
     * 从认证信息中提取用户ID
     * TODO: 根据实际的安全实现调整此方法
     */
    private Integer getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            // 临时：默认返回用户ID 1，待与现有安全系统集成后修改
            return 1;
        }

        // 根据实际的Principal类型提取用户ID
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number) {
            return ((Number) principal).intValue();
        }

        // 默认返回
        return 1;
    }
}
