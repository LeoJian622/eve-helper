package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;

/**
 * SysMenu 实体转换器
 *
 * PO↔domain 映射已下沉至基础设施层(原 PO 方法为死代码,已移除)
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysMenuAssembler {

}
