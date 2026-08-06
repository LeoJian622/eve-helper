package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;

/**
 * 钱包日志转换器
 *
 * PO↔domain 映射已下沉至基础设施层
 * {@link xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.WalletJournalPoConverter}
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletJournalAssembler {

}
