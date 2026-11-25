package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.response.CharacterPanelResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.CharacterSkillsResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.SkillQueueResponse;
import xyz.foolcat.eve.evehelper.application.service.CharacterPanelApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPanelInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 角色面板控制器(022 US1)。
 *
 * <p>三端点:面板(panel)、技能(skills)、技能队列(skill-queue)。
 * 消费 {@link CharacterPanelApplicationService} 三方法,
 * 将领域 VO 转为响应 DTO。技能队列端点在控制器层批量解析 skillName
 * (via {@link InvTypesService#getNameByTypeIds})。</p>
 *
 * <p>异常原样传播,由 {@link GlobalExceptionHandler} 统一处理
 * (归属违规 → 403 等)。</p>
 *
 * @author Leojan
 */
@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
public class CharacterPanelController {

    private final CharacterPanelApplicationService panelService;
    private final InvTypesService invTypesService;

    /**
     * 角色面板:公开信息 + 在线 + 位置 + 舰船。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = CharacterPanelResponse
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/panel")
    public Result<CharacterPanelResponse> panel(
            @PathVariable Integer characterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        CharacterPanelInfo info = panelService.panel(characterId, forceRefresh);
        return Result.success(CharacterPanelResponse.from(info));
    }

    /**
     * 技能视图:属性 + 技能列表。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = CharacterSkillsResponse
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/skills")
    public Result<CharacterSkillsResponse> skills(
            @PathVariable Integer characterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        CharacterSkillsInfo info = panelService.skills(characterId, forceRefresh);
        return Result.success(CharacterSkillsResponse.from(info));
    }

    /**
     * 技能队列:队列条目(含 skillName 批量解析) + queueEndsAt。
     *
     * <p>skillName 解析:收集队列全部 skillId → 一次
     * {@code invTypesService.getNameByTypeIds(list)} → 组装条目;
     * 解析不到的 skillId 对应 null(不抛异常);空队列跳过解析。</p>
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = SkillQueueResponse
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/skill-queue")
    public Result<SkillQueueResponse> skillQueue(
            @PathVariable Integer characterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        SkillQueueInfo info = panelService.skillQueue(characterId, forceRefresh);

        // 批量解析 skillName:空队列跳过;非空队列收集全部 skillId 一次查询
        Map<Integer, String> nameMap;
        List<SkillQueueItem> queue = info.queue();
        if (queue.isEmpty()) {
            nameMap = Collections.emptyMap();
        } else {
            List<Integer> skillIds = queue.stream()
                    .map(SkillQueueItem::skillId)
                    .toList();
            nameMap = invTypesService.getNameByTypeIds(skillIds);
        }

        return Result.success(SkillQueueResponse.from(info, nameMap));
    }
}
