package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;

/**
 * ESI配置转换器
 *
 * PO↔domain 映射已下沉至基础设施层
 * {@link xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.EsiConfigPoConverter}
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiConfigAssembler {

}
