package xyz.foolcat.eve.evehelper.application.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.application.query.handler.QueryHandler;
import xyz.foolcat.eve.evehelper.application.query.model.Query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询总线
 *
 * @author yongj
 * date 2025-08-01 15:28
 */

@Component
public class QueryBus implements InitializingBean {

    private final List<QueryHandler<?,?>> handlers;
    private final Map<Class<?>, QueryHandler<?,?>> queryHandlers = new HashMap<>();

    public QueryBus(List<QueryHandler<?,?>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        handlers.forEach(handler -> {
            Class<?> queryType = getQueryType(handler);
            if (queryType != null) {
                queryHandlers.put(queryType, handler);
            }
        });
    }

    private Class<?> getQueryType(QueryHandler<?,?> handler) {
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType().equals(QueryHandler.class)) {
                    Type queryType = parameterizedType.getActualTypeArguments()[0];
                    if (queryType instanceof Class) {
                        return (Class<?>) queryType;
                    }
                }
            }
        }
        return null;
    }

    public <R> R dispatch(Query<R> query) {
        QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) queryHandlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for query: " + query.getClass().getSimpleName());
        }
        return handler.handle(query);
    }
}
