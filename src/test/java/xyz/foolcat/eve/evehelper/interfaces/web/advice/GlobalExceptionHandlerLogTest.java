package xyz.foolcat.eve.evehelper.interfaces.web.advice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GlobalExceptionHandler BindException 日志加固测试(008 T015 / research R9,HIGH-1)。
 *
 * <p>验证 DTO 边界拒绝路径的日志为结构化摘要,不含 rejected value(攻击者可控载荷,CRLF 注入面),
 * 不打印异常对象本体;级别 warn(非 error)。契约 C-2 断言 4。
 */
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerLogTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void bindExceptionLogDoesNotContainRejectedValue(CapturedOutput output) {
        // 构造含 CRLF 的 rejected value(10KB/CRLF 载荷攻击面)
        BindException be = new BindException(new Object(), "login");
        be.addError(new FieldError("login", "refreshToken",
                "attack\r\nforged-log-line", true, null, null, "长度不能超过64"));

        handler.processException(be);

        String log = output.getAll();
        assertFalse(log.contains("attack"), "日志不得含 rejected value 原文(CRLF 注入面)");
        assertFalse(log.contains("forged-log-line"), "伪造的日志行片段不得进入日志");
        assertTrue(log.contains("login"), "日志应含 objectName");
        assertTrue(log.contains("refreshToken"), "日志应含字段名");
        assertTrue(log.contains("长度不能超过64"), "日志应含约束消息摘要");
        assertFalse(log.toUpperCase().contains(" ERROR "), "该分支应为 warn 级别,而非 error");
    }

    @Test
    void typeMismatchLogDoesNotContainRawInput(CapturedOutput output) {
        // 008 T018(LOW-D):typeMismatch 分支 toString 回显原始输入(仅认证后可达),推广 R9 原则
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "attack-value", Integer.class, "id", null, null);

        handler.processException(e);

        String log = output.getAll();
        assertFalse(log.contains("attack-value"), "日志不得含原始输入(typeMismatch toString 面)");
        assertTrue(log.contains("id"), "日志应含参数名(非用户输入)");
    }
}