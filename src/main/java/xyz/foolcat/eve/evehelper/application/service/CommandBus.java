package xyz.foolcat.eve.evehelper.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.application.command.handler.CommandHandler;
import xyz.foolcat.eve.evehelper.application.command.model.Command;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令总线
 *
 * @author yongj
 * date 2025-08-01 16:15
 */

@Component
public class CommandBus implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandBus.class);

    private final List<CommandHandler<?,?>> handlers;

    private final Map<Class<?>, CommandHandler<?,?>> commandHandlers = new HashMap<>();

    public CommandBus(List<CommandHandler<?,?>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 在这里进行初始化和注册
        handlers.forEach(handler -> {
            Class<?> commandType = getCommandType(handler);
            if (commandType != null) {
                commandHandlers.put(commandType, handler);
            }
        });
    }

    // 泛型类型推断的核心方法
    private Class<?> getCommandType(CommandHandler<?,?> handler) {
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType().equals(CommandHandler.class)) {
                    // 获取CommandHandler<C, R>中的第一个泛型参数C
                    Type commandType = parameterizedType.getActualTypeArguments()[0];
                    if (commandType instanceof Class) {
                        return (Class<?>) commandType;
                    }
                }
            }
        }
        return null;
    }

    public <R> R dispatch(Command<R> command) {
        CommandHandler<?, ?> rawHandler = commandHandlers.get(command.getClass());
        if (rawHandler == null) {
            logger.error("No handler found for command: {}", command.getClass().getSimpleName());
            throw new IllegalArgumentException("No handler found for command: " + command.getClass().getSimpleName());
        }
        
        try {
            logger.debug("Dispatching command: {}", command.getClass().getSimpleName());
            @SuppressWarnings("unchecked")
            CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) rawHandler;
            R result = handler.handle(command);
            logger.debug("Command {} handled successfully", command.getClass().getSimpleName());
            return result;
        } catch (Exception e) {
            logger.error("Error handling command: {}", command.getClass().getSimpleName(), e);
            throw new RuntimeException("Error handling command: " + command.getClass().getSimpleName(), e);
        }
    }
}
