package xyz.foolcat.eve.evehelper.interfaces.web.advice;


import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import javax.servlet.ServletException;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.rmi.ServerException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器
 *
 * @author Leojan
 * date 2021-08-11 9:57
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 表单绑定到 java bean 出错时抛出 BindException 异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public <T> Result<T> processException(BindException e) {
        log.error("表单绑定异常: ", e);
        JSONObject msg = new JSONObject();
        e.getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                msg.set(fieldError.getField(),
                        fieldError.getDefaultMessage());
            } else {
                msg.set(error.getObjectName(),
                        error.getDefaultMessage());
            }
        });
        return Result.failed(ResultCode.PARAM_ERROR, msg.toString());
    }

    /**
     * 普通参数(非 java bean)校验出错时抛出 ConstraintViolationException 异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public <T> Result<T> processException(ConstraintViolationException e) {
        log.error("参数校验异常: ", e);
        JSONObject msg = new JSONObject();
        e.getConstraintViolations().forEach(constraintViolation -> {
            String template = constraintViolation.getMessage();
            String path = constraintViolation.getPropertyPath().toString();
            msg.set(path, template);
        });
        return Result.failed(ResultCode.PARAM_ERROR, msg.toString());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public <T> Result<T> processException(ValidationException e) {
        log.error("验证异常: ", e);
        return Result.failed(ResultCode.PARAM_ERROR, "参数校验失败");
    }


    /**
     * MissingServletRequestParameterException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public <T> Result<T> processException(MissingServletRequestParameterException e) {
        log.error("缺少必需参数异常: ", e);
        return Result.failed(ResultCode.PARAM_IS_NULL);
    }

    /**
     * MethodArgumentTypeMismatchException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public <T> Result<T> processException(MethodArgumentTypeMismatchException e) {
        log.error("方法参数类型不匹配异常: ", e);
        return Result.failed(ResultCode.PARAM_ERROR, "类型错误");
    }

    /**
     * ServletException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ServerException.class)
    public <T> Result<T> processException(ServletException e) {
        log.error("服务器异常: ", e);
        return Result.failed(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IllegalArgumentException.class})
    public Result handlerIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常，异常原因：{}", e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(JsonProcessingException.class)
    public Result handleJsonProcessingException(JsonProcessingException e) {
        log.error("Json转换异常: ", e);
        return Result.failed("JSON格式错误");
    }

    /**
     * HttpMessageNotReadableException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public <T> Result<T> processException(HttpMessageNotReadableException e) {
        log.error("HTTP消息不可读异常: ", e);
        String errorMessage = "请求体不可为空";
        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage = convertMessage(cause);
        }
        return Result.failed(errorMessage);
    }

    /**
     * TypeMismatchException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException.class)
    public <T> Result<T> processException(TypeMismatchException e) {
        log.error("类型不匹配异常: ", e);
        return Result.failed("参数类型错误");
    }

    /**
     * 数据库访问异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DataAccessException.class)
    public <T> Result<T> processException(DataAccessException e) {
        log.error("数据库访问异常: ", e);
        return Result.failed("数据访问异常，请稍后重试");
    }

    /**
     * ESI接口异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EsiException.class)
    public Result handleEsiException(EsiException e) {
        log.error("ESI接口异常: ", e);
        return Result.result(e.getResultCode().getCode(), e.getMessage(), null);
    }

    /**
     * WebClient请求异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebClientRequestException.class)
    public Result handleWebClientRequestException(WebClientRequestException e) {
        log.error("WebClient请求异常: ", e);
        return Result.failed("网络请求异常，请稍后重试");
    }

    /**
     * WebClient响应异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebClientResponseException.class)
    public Result handleWebClientResponseException(WebClientResponseException e) {
        log.error("WebClient响应异常: ", e);
        return Result.failed("外部服务响应异常，请稍后重试");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未知异常: ", e);
        return Result.failed("系统内部错误，请联系管理员");
    }


    /**
     * 传参类型错误时，用于消息转换
     *
     * @param throwable 异常
     * @return 错误信息
     */
    private String convertMessage(Throwable throwable) {
        String error = throwable.toString();
        String regulation = "\\[\"(.*?)\"]+";
        Pattern pattern = Pattern.compile(regulation);
        Matcher matcher = pattern.matcher(error);
        String group = "";
        if (matcher.find()) {
            String matchString = matcher.group();
            matchString = matchString
                    .replace("[", "")
                    .replace("]", "");
            matchString = matchString.replaceAll("\\\"", "") + "字段类型错误";
            group += matchString;
        }
        return group;
    }
}