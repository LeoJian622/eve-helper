package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.List;

/**
 * ESI 宇宙相关信息接口
 *
 * @author Leojan
 * date 2023-11-18 22:37
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 宇宙相关信息接口")
public class UniverseApi {

    private final WebClient apiClient;
    private final PageTotalApi pageTotalApi;

    /**
     * 获取人物民族
     *
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-获取人物民族")
    public Flux<AncestriesResponse> queryUniverseAncestries(String datasource, String language) {
        return apiClient.get().uri("/universe/ancestries/?datasource={datasource}&language={language}", datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AncestriesResponse.class);
    }

    /**
     * 查询小行星带信息
     *
     * @param asteroidBeltId 小行星带ID
     * @param datasource     服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "asteroidBeltId", description = "小行星带ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-查询小行星带信息")
    public Mono<AsteroidBeltInformationResponse> queryUniverseAsteroidBelts(Integer asteroidBeltId, String datasource) {
        return apiClient.get().uri("/universe/asteroid_belts/{asteroid_belt_id}/?datasource={datasource}", asteroidBeltId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(AsteroidBeltInformationResponse.class);
    }

    /**
     * 获取种族信息
     *
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-获取种族信息")
    public Flux<BloodlineResponse> queryUniverseBloodlines(String datasource, String language) {
        return apiClient.get().uri("/universe/bloodlines/?datasource={datasource}&language={language}", datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BloodlineResponse.class);
    }

    /**
     * 类别Id列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-类别Id列表")
    public Flux<Integer> queryUniverseCategories(String datasource) {
        return apiClient.get().uri("/universe/categories/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 类别详情
     *
     * @param categoryId 类别ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "categoryId", description = "类别ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-类别详情")
    public Mono<CategoryInfoResponse> queryUniverseCategory(Integer categoryId, String datasource, String language) {
        return apiClient.get().uri("/universe/categories/{category_id}/?datasource={datasource}&language={language}", categoryId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CategoryInfoResponse.class);
    }

    /**
     * 星座Id列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星座Id列表")
    public Flux<Integer> queryUniverseConstellations(String datasource) {
        return apiClient.get().uri("/universe/constellations/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 星座详情
     *
     * @param constellationId 星座ID
     * @param datasource      服务器数据源
     * @param language        语言
     * @return
     */
    @Parameters({
            @Parameter(name = "constellationId", description = "星座ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-星座详情")
    public Mono<ConstellationInfoResponse> queryUniverseConstellation(Integer constellationId, String datasource, String language) {
        return apiClient.get().uri("/universe/constellations/{constellation_id}/?datasource={datasource}&language={language}",
                        constellationId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(ConstellationInfoResponse.class);
    }

    /**
     * 势力列表
     *
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-势力列表")
    public Flux<FactionResponse> queryUniverseFactions(String datasource, String language) {
        return apiClient.get().uri("/universe/factions/?datasource={datasource}&language={language}", datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FactionResponse.class);
    }

    /**
     * 图标ID列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-图标ID列表")
    public Flux<Integer> queryUniverseGraphics(String datasource) {
        return apiClient.get().uri("/universe/graphics/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 图标详情
     *
     * @param graphicId  图标ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "graphicId", description = "图标ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-图标详情")
    public Mono<GraphicResponse> queryUniverseGraphic(Integer graphicId, String datasource, String language) {
        return apiClient.get().uri("/universe/graphics/{graphic_id}/?datasource={datasource}&language={language}",
                        graphicId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(GraphicResponse.class);
    }

    /**
     * 某物品分组ID列表最大页数
     *
     * @param datasource 服务器
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-某物品分组ID列表最大页数")
    public Integer queryUniverseGroupsMaxPage(String datasource) {
        String uri = "/universe/groups/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage("", uri,  apiClient);
    }

    /**
     * 物品分组ID列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-物品分组ID列表")
    public Flux<Integer> queryUniverseGroups(String datasource) {
        return apiClient.get().uri("/universe/groups/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 物品分组详情
     *
     * @param groupId    物品分组ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "graphicId", description = "物品分组ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-物品分组详情")
    public Mono<GroupInfoResponse> queryUniverseGroup(Integer groupId, String datasource, String language) {
        return apiClient.get().uri("/universe/groups/{group_id}/?datasource={datasource}&language={language}",
                        groupId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(GroupInfoResponse.class);
    }

    /**
     * 解析名称的ID
     *
     * @param names      名称
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "names", description = "名称", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-解析名称的ID")
    public Mono<Name2IdResponse> queryUniverseIds(List<String> names, String datasource, String language) {
        return apiClient.post().uri("/universe/ids/?datasource={datasource}&language={language}",
                        datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .body(Mono.just(names), List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Name2IdResponse.class);
    }

    /**
     * 卫星详情
     *
     * @param moonId     卫星ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "moonId", description = "卫星ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-卫星详情")
    public Mono<MoonInfoResponse> queryUniverseMoon(Integer moonId, String datasource) {
        return apiClient.get().uri("/universe/moons/{moon_id}/?datasource={datasource}", moonId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(MoonInfoResponse.class);
    }

    /**
     * 根据ID获取名称和分类
     *
     * @param ids        ID列表
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "ids", description = "ID列表", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-根据ID获取名称和分类")
    public Flux<Id2NameResponse> queryUniverseNames(List<Integer> ids, String datasource) {
        return apiClient.post().uri("/universe/names/?datasource={datasource}", datasource)
                .body(Mono.just(ids), List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Id2NameResponse.class);
    }

    /**
     * 行星详情
     *
     * @param planetId   行星ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "planetId", description = "行星ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-行星详情")
    public Mono<PlanetInfoResponse> queryUniversePlanet(Integer planetId, String datasource) {
        return apiClient.get().uri("/universe/planets/{planet_id}/?datasource={datasource}", planetId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(PlanetInfoResponse.class);
    }


    /**
     * 人物种族列表
     *
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-人物种族列表")
    public Flux<RaceInfoResponse> queryUniverseRaces(String datasource, String language) {
        return apiClient.get().uri("/universe/races/?datasource={datasource}&language={language}", datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(RaceInfoResponse.class);
    }

    /**
     * 星域列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星域列表")
    public Flux<Integer> queryUniverseRegions(String datasource) {
        return apiClient.get().uri("/universe/regions/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 星域详情
     *
     * @param regionId   星域ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "regionId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-星域详情")
    public Mono<RegionInfoResponse> queryUniverseRegion(Integer regionId, String datasource, String language) {
        return apiClient.get().uri("/universe/regions/{region_id}/?datasource={datasource}&language={language}", regionId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(RegionInfoResponse.class);
    }

    /**
     * 星门详情
     *
     * @param stargateId 星门ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "stargateId", description = "星门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星门详情")
    public Mono<StargateInfoResponse> queryUniverseStargate(Integer stargateId, String datasource) {
        return apiClient.get().uri("/universe/stargates/{stargate_id}/?datasource={datasource}", stargateId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(StargateInfoResponse.class);
    }

    /**
     * 恒星详情
     *
     * @param starId     星门ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "starId", description = "星门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星门详情")
    public Mono<StarInfoResponse> queryUniverseStar(Integer starId, String datasource) {
        return apiClient.get().uri("/universe/stars/{star_id}/?datasource={datasource}", starId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(StarInfoResponse.class);
    }

    /**
     * 空间站详情
     *
     * @param stationId  空间站ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "stationId", description = "空间站ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-空间站详情")
    public Mono<StationInfoResponse> queryUniverseStation(Integer stationId, String datasource) {
        return apiClient.get().uri("/universe/stations/{station_id}/?datasource={datasource}", stationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(StationInfoResponse.class);
    }

    /**
     * 公开建筑列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-公开建筑列表")
    public Flux<Long> queryUniverseStructures(String datasource) {
        return apiClient.get().uri("/universe/structures/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 建筑详情
     * 如果位于 ACL 上，则返回有关请求结构的信息。否则，对所有输入返回“Forbidden”。
     *
     * @param structureId   建筑ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "structureId", description = "建筑ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-建筑详情")
    public Mono<StructureInfoResponse> queryUniverseStructure(Long structureId, String datasource, String accessesToken) {
        return apiClient.get().uri("/universe/structures/{structure_id}/?datasource={datasource}", structureId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(StructureInfoResponse.class);
    }

    /**
     * 过去一小时星系中跳跃数
     * 获取以 Last-Modified 标头的时间戳结束的过去一小时内太阳系的跳跃次数，不包括虫洞空间。仅列出具有跳跃功能的系统
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-过去一小时星系中跳跃数")
    public Flux<SolarSystemJumpResponse> queryUniverseSystemJumps(String datasource) {
        return apiClient.get().uri("/universe/system_jumps/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(SolarSystemJumpResponse.class);
    }

    /**
     * 过去一小时星系中击毁舰船、太空舱和NPC的数量
     * 获取每个太阳系在以 Last-Modified 标头的时间戳结束的过去一小时内杀死的舰船、太空舱和 NPC 的数量，不包括虫洞空间。只有具有杀戮功能的系统才会被列出
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-过去一小时星系中击毁舰船、太空舱和NPC的数量")
    public Flux<SolarSystemKillResponse> queryUniverseSystemKills(String datasource) {
        return apiClient.get().uri("/universe/system_kills/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(SolarSystemKillResponse.class);
    }

    /**
     * 星系列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星系列表")
    public Flux<Integer> queryUniverseSystems(String datasource) {
        return apiClient.get().uri("/universe/systems/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 恒星系详情
     *
     * @param solarSystemId 恒星系ID
     * @param datasource    服务器数据源
     * @param language      语言
     * @return
     */
    @Parameters({
            @Parameter(name = "solarSystemId", description = "恒星系ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-恒星系详情")
    public Mono<SolarSystemInfoResponse> queryUniverseSystem(Integer solarSystemId, String datasource, String language) {
        return apiClient.get().uri("/universe/systems/{system_id}/?datasource={datasource}&language={language}", solarSystemId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(SolarSystemInfoResponse.class);
    }

    /**
     * 物品类型ID的列表最大页数
     *
     * @param datasource 服务器
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-物品类型ID的列表最大页数")
    public Integer queryUniverseTypesUnMaxPage(String datasource) {
        String uri = "/universe/types/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage("", uri,  apiClient);
    }

    /**
     * 获取物品类型ID的列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-获取物品类型ID的列表")
    public Flux<Integer> queryUniverseTypes(String datasource, Integer page) {
        return apiClient.get().uri("/universe/types/?datasource={datasource}&page={page}", datasource, page)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 物品类型详情
     *
     * @param typeId     物品类型ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "typeId", description = "物品类型ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-物品类型详情")
    public Mono<TypeInfoResponse> queryUniverseType(Integer typeId, String datasource, String language) {
        return apiClient.get().uri("/universe/types/{type_id}/?datasource={datasource}&language={language}", typeId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(TypeInfoResponse.class);
    }
}
