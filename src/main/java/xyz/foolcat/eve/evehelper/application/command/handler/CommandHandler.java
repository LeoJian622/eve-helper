package xyz.foolcat.eve.evehelper.application.command.handler;

import xyz.foolcat.eve.evehelper.application.command.model.Command;

/**
 * 泛型命令处理器接口
 *
 * @author yongj
 * date 2025-08-11 11:36
 */

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}