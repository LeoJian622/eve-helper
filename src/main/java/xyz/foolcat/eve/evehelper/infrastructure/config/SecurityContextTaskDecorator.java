package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 线程池任务身份快照传播装饰器。
 *
 * <p><b>解决的问题</b>:移除 {@code MODE_INHERITABLETHREADLOCAL} 后,池化/复用工作线程不再在创建时
 * 继承提交线程身份,但也不会自动清理——若任务不显式设置身份,可能读到上一次任务残留的身份(跨用户泄漏,007 MEDIUM-6),
 * 或请求线程身份被错误带入异步任务。
 *
 * <p><b>语义</b>(research.md R1):
 * <ol>
 *   <li>提交线程有身份 -> 快照传播给工作线程;</li>
 *   <li>执行前 {@code clearContext} 清除工作线程残留,执行后 {@code finally clearContext} 保证恒空;</li>
 *   <li><b>同线程直通</b>:提交线程 == 执行线程({@code CallerRunsPolicy})时直接执行,不触碰上下文,
 *       避免抹掉请求线程自身身份(防 {@code AccessGuard} fail-closed);</li>
 *   <li>快照为 null(匿名/调度线程提交)-> 任务匿名执行;</li>
 *   <li>快照为 {@code createEmptyContext()} 副本,提交线程后续变更不被工作线程观察。</li>
 * </ol>
 *
 * @author Leojan
 * date 2026-08-13
 */
public class SecurityContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Thread submittingThread = Thread.currentThread();
        SecurityContext snapshot = SecurityContextHolder.createEmptyContext();
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            snapshot.setAuthentication(context.getAuthentication());
        }
        return () -> {
            if (Thread.currentThread() == submittingThread) {
                runnable.run();
                return;
            }
            SecurityContextHolder.clearContext();
            if (snapshot.getAuthentication() != null) {
                SecurityContextHolder.setContext(snapshot);
            }
            try {
                runnable.run();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}