package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.MemberTrackingApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingInfo;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * 团员追踪控制器(T038, US5)。
 * <p>
 * 薄适配器,零业务逻辑,委托 {@link MemberTrackingApplicationService}。
 *
 * @author Leojian
 */
@RestController
@RequestMapping("/corporation")
@RequiredArgsConstructor
public class MemberTrackingController {

    private final MemberTrackingApplicationService memberTrackingApplicationService;

    /**
     * 查询军团成员追踪信息。
     *
     * @param corpId       军团 ID(路径参数)
     * @param characterId  人物 ID(必填,Director 角色)
     * @param forceRefresh 是否强制刷新(默认 false)
     * @return 团员追踪聚合 VO 信封
     */
    @GetMapping("/{corpId}/member-tracking")
    public Result<MemberTrackingInfo> track(
            @PathVariable Integer corpId,
            @RequestParam Integer characterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) throws ParseException {
        return Result.success(memberTrackingApplicationService.track(corpId, characterId, forceRefresh));
    }
}
