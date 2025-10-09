package xyz.foolcat.eve.evehelper.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(QueryBus.class);

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
        QueryHandler<?, ?> rawHandler = queryHandlers.get(query.getClass());
        if (rawHandler == null) {
            logger.error("No handler found for query: {}", query.getClass().getSimpleName());
            throw new IllegalArgumentException("No handler found for query: " + query.getClass().getSimpleName());
        }
        
        try {
            logger.debug("Dispatching query: {}", query.getClass().getSimpleName());
            @SuppressWarnings("unchecked")
            QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) rawHandler;
            R result = handler.handle(query);
            logger.debug("Query {} handled successfully", query.getClass().getSimpleName());
            return result;
        } catch (Exception e) {
            logger.error("Error handling query: {}", query.getClass().getSimpleName(), e);
            throw new RuntimeException("Error handling query: " + query.getClass().getSimpleName(), e);
        }
    }
}
