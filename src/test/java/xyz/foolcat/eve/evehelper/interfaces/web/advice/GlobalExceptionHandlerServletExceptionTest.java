package xyz.foolcat.eve.evehelper.interfaces.web.advice;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.rmi.ServerException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler ServletException 处理器声明正确性测试(缺陷修复)。
 *
 * <p>发现:原 {@code @ExceptionHandler(java.rmi.ServerException.class)} 声明的是 RMI 异常,
 * 而方法参数却是 {@link jakarta.servlet.ServletException}——类型错乱,导致该处理器既匹配不到
 * RMI ServerException(参数装不下),也匹配不到业务实际会抛的 ServletException(声明值不匹配),
 * 处理器实际是死的。本测试以反射断言注解声明类型,精确捕捉该框架映射错误。
 */
class GlobalExceptionHandlerServletExceptionTest {

    private static final String PROCESS_METHOD = "processException";

    @Test
    void servletExceptionHandlerTargetsServletExceptionNotRmiServerException() throws NoSuchMethodException {
        var method = GlobalExceptionHandler.class.getMethod(PROCESS_METHOD, jakarta.servlet.ServletException.class);
        var ann = method.getAnnotation(ExceptionHandler.class);

        assertThat(ann)
                .as("processException 必须声明为 ServletException 处理器")
                .isNotNull();
        assertThat(ann.value())
                .as("注解声明类型必须包含 ServletException")
                .contains(jakarta.servlet.ServletException.class);
        assertThat(ann.value())
                .as("注解声明类型不得是 java.rmi.ServerException(RMI 异常,业务不会抛)")
                .doesNotContain(ServerException.class);
    }
}