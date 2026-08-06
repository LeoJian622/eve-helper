package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;

import java.util.List;

/**
 * EVE账户转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EveAccountAssembler {

    /**
     * EveAccount 转换为 UserAccountTO
     * @param eveAccount
     * @return
     */
    List<UserAccountDTO> domain2UserAccountTO(List<EveAccount> eveAccount);
}
