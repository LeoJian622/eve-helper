package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.request.MarkMailReadRequest;
import xyz.foolcat.eve.evehelper.application.dto.request.ResolveNamesRequest;
import xyz.foolcat.eve.evehelper.application.dto.request.SendMailRequest;
import xyz.foolcat.eve.evehelper.application.dto.response.ResolveNamesResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.SendMailResponse;
import xyz.foolcat.eve.evehelper.application.service.MailApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * 邮件控制器(022 US2)。
 *
 * <p>六端点:收件箱(inbox)、标签(labels)、详情(detail)、
 * 标记已读(markRead)、发送(send)、名称解析(resolveNames)。</p>
 *
 * <p>异常原样传播,由 {@link GlobalExceptionHandler} 统一处理
 * (归属违规 → 403、权限不足 → 既有映射等)。</p>
 *
 * <p>resolveNames 公开:无 authorize、无 token。</p>
 *
 * @author Leojan
 */
@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailApplicationService mailService;

    /**
     * 收件箱:lastMailId 游标可选(缺省不传)。
     *
     * @param characterId  角色 ID
     * @param lastMailId   游标(可选);首页不传
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = MailHeaderPage
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/inbox")
    public Result<MailHeaderPage> inbox(
            @PathVariable Integer characterId,
            @RequestParam(required = false) Integer lastMailId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        MailHeaderPage page = mailService.inbox(characterId, lastMailId, forceRefresh);
        return Result.success(page);
    }

    /**
     * 标签:返回标签列表与总未读数。
     *
     * @param characterId  角色 ID
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = MailLabelsInfo
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/labels")
    public Result<MailLabelsInfo> labels(
            @PathVariable Integer characterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        MailLabelsInfo info = mailService.labels(characterId, forceRefresh);
        return Result.success(info);
    }

    /**
     * 详情:返回原始正文。
     *
     * @param characterId  角色 ID
     * @param mailId       邮件 ID
     * @param forceRefresh true 时强制刷新缓存;缺省 false
     * @return Result 信封, data = MailDetailInfo
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @GetMapping("/{characterId}/{mailId}")
    public Result<MailDetailInfo> detail(
            @PathVariable Integer characterId,
            @PathVariable Integer mailId,
            @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        MailDetailInfo info = mailService.detail(characterId, mailId, forceRefresh);
        return Result.success(info);
    }

    /**
     * 标记已读/未读。
     *
     * @param characterId 角色 ID
     * @param mailId      邮件 ID
     * @param request     标记请求(@Valid; read @NotNull)
     * @return Result 信封, 无 data
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @PutMapping("/{characterId}/{mailId}/read")
    public Result<Void> markRead(
            @PathVariable Integer characterId,
            @PathVariable Integer mailId,
            @Valid @RequestBody MarkMailReadRequest request) throws ParseException {
        mailService.markRead(characterId, mailId, request.read());
        return Result.success();
    }

    /**
     * 发送邮件。
     *
     * @param characterId 角色 ID
     * @param request     发送请求(@Valid; 校验规则见 data-model §6)
     * @return Result 信封, data = SendMailResponse(mailId)
     * @throws ParseException JWT 解析失败(由 GlobalExceptionHandler 处理)
     */
    @PostMapping("/{characterId}/send")
    public Result<SendMailResponse> send(
            @PathVariable Integer characterId,
            @Valid @RequestBody SendMailRequest request) throws ParseException {
        Integer mailId = mailService.send(characterId, request);
        return Result.success(new SendMailResponse(mailId));
    }

    /**
     * 名称解析(公开,无授权)。
     *
     * @param request 名称解析请求(@Valid; names @NotEmpty @Size(max=50))
     * @return Result 信封, data = ResolveNamesResponse
     */
    @PostMapping("/recipients/resolve")
    public Result<ResolveNamesResponse> resolveNames(
            @Valid @RequestBody ResolveNamesRequest request) {
        return Result.success(new ResolveNamesResponse(mailService.resolveNames(request.names())));
    }
}
