package xyz.foolcat.eve.evehelper.application.dto.response;

import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;

import java.util.List;

/**
 * 名称解析响应 DTO(022 US2)。
 *
 * @param results 解析结果列表(全量, 含未命中项)
 * @author Leojan
 */
public record ResolveNamesResponse(List<RecipientResolution> results) {
}
