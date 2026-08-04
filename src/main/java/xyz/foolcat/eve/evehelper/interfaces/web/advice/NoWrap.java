package xyz.foolcat.eve.evehelper.interfaces.web.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过包装的注解
 *
 * @author Leojan
 * date 2026-07-31 22:49
 */

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoWrap {
}
