package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 错误响应体
 *
 * @author Leojan
 * @date 2023-08-02 11:52
 */

@Data
@Tag(name = "错误响应体 40X 50X object")
public class ErrorResponse {

    private String error;

    private Integer timeout;

    @JsonProperty("error_description")
    private String errorDescription;
}
