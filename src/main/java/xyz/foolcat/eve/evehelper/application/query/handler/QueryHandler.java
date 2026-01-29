package xyz.foolcat.eve.evehelper.application.query.handler;


import xyz.foolcat.eve.evehelper.application.query.model.Query;

/**
 * 泛型查询处理器接口
 *
 * @author yongj
 * date 2025-08-11 11:33
 */

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
