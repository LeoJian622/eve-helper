package xyz.foolcat.eve.evehelper.shared.kernel.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yongj
 * date 2025-08-11 11:31
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface QueryHandlers {
}
