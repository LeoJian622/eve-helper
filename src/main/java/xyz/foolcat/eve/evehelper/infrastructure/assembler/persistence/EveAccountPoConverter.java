package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;

import java.util.List;

/**
 * EveAccount 领域实体与 EveAccountPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EveAccountPoConverter {

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

    /**
     * EveAccount 转换为 EveAccountPO
     * @param eveAccount
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    List<EveAccountPO> domain2Po(List<EveAccount> eveAccount);

    /**
     * EveAccountPO 转换为 EveAccount
     * @param eveAccountPO
     * @return
     */
    List<EveAccount> po2Domain(List<EveAccountPO> eveAccountPO);
}
