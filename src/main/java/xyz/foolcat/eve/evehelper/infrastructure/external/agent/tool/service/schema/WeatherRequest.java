package xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.schema;

import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 示例：天气工具请求对象
 *
 * @author yongj
 * date 2026-04-14 14:26
 */

public record WeatherRequest(@ToolParam(description = "The name of a city or a country") String location, @ToolParam(description = "the temperature unit is Celsius(C) or Fahrenheit(F)")String unit) {
}
