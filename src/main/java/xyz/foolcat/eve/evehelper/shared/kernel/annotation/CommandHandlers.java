package xyz.foolcat.eve.evehelper.shared.kernel.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yongj
 * date 2025-08-11 11:30
 */

@Target(ElementType.TYPE) // 注解作用于类上
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时可用
@Documented
@Component // 标记为 Spring Bean
public @interface CommandHandlers {
}
