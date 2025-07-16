package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;

/**
 * EVE账户转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EveAccountAssembler {

    /**
     * EveAccountPO 转换为 EveAccount
     * @param eveAccountPO
     * @return
     */
    EveAccount po2Domain(EveAccountPO eveAccountPO);

    /**
     * EveAccount 转换为 EveAccountPO
     * @param eveAccount
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    EveAccountPO domain2Po(EveAccount eveAccount);
} 