package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.AuthErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.BookmarkFoldersResponse;
import xyz.foolcat.eve.evehelper.esi.model.BookmarksResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 位标相关接口
 * @author Leojan
 * @date 2023-09-28 10:58
 */

@Service
@RequiredArgsConstructor
public class BookmarksApi {

    private final WebClient apiClient;

    /**
     *
     * 查询角色个人位标列表
     *
     * @param characterId 角色ID
     * @param datasource 服务器
     * @param page 页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId",description = "角色ID" ,required = true),
            @Parameter(name = "datasource",description = "serenity" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-角色个人位标列表")
    public Flux<BookmarksResponse> queryCharactersBookmarks(Long characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/bookmarks/?datasource={datasource}&page={page}",characterId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BookmarksResponse.class);
    }

    /**
     *
     * 查询角色个人位标文件夹
     *
     * @param characterId 角色ID
     * @param datasource 服务器
     * @param page 页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId",description = "角色ID" ,required = true),
            @Parameter(name = "datasource",description = "serenity" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-角色个人位标列表")
    public Flux<BookmarkFoldersResponse> queryCharactersBookmarksFolders(Long characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/bookmarks/folders/?datasource={datasource}&page={page}",characterId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BookmarkFoldersResponse.class);
    }
}
