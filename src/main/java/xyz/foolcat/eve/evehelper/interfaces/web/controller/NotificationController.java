package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.response.NotificationItem;
import xyz.foolcat.eve.evehelper.application.dto.response.UnreadCountResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.WsTicketResponse;
import xyz.foolcat.eve.evehelper.application.service.NotificationApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 通知控制器(022 US4)。
 *
 * <p>四端点:分页(page)、未读计数(unreadCount)、标记已读(markRead)、
 * WS 票据(wsTicket)。薄适配器,零业务逻辑。</p>
 *
 * @author Leojian
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationApplicationService notificationService;

    /**
     * 分页查询通知。
     *
     * @param current    页码(默认 1)
     * @param size       每页大小(默认 20)
     * @param unreadOnly true=仅未读;缺省=全部
     * @return Result 信封,data = PageResult
     */
    @GetMapping
    public Result<PageResult<NotificationItem>> page(
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(1000) Integer size,
            @RequestParam(required = false) Boolean unreadOnly) {
        PageQuery pageQuery = PageQuery.builder().current(current).size(size).build();
        return Result.success(notificationService.page(unreadOnly, pageQuery));
    }

    /**
     * 未读通知计数。
     *
     * @return Result 信封,data = UnreadCountResponse
     */
    @GetMapping("/unread-count")
    public Result<UnreadCountResponse> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    /**
     * 标记通知已读。
     *
     * @param id 通知ID
     * @return Result 信封,无 data
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    /**
     * 创建 WebSocket 连接票据。
     *
     * @return Result 信封,data = WsTicketResponse
     */
    @PostMapping("/ws-ticket")
    public Result<WsTicketResponse> wsTicket() {
        return Result.success(notificationService.createWsTicket());
    }
}
