package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;

import java.util.List;

/**
 * InvTypes 领域实体与 InvTypesPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface InvTypesPoConverter {

    /**
     * InvTypesPO 转换为 InvTypes
     * @param invTypesPO
     * @return
     */
    InvTypes po2Domain(InvTypesPO invTypesPO);

    /**
     * InvTypes 转换为 InvTypesPO
     * @param invTypes
     * @return
     */
    InvTypesPO domain2Po(InvTypes invTypes);

    /**
     * InvTypesPO 列表转换为 InvTypes 列表
     * @param invTypesPOs
     * @return
     */
    List<InvTypes> po2Domain(List<InvTypesPO> invTypesPOs);

    /**
     * InvTypes 列表转换为 InvTypesPO 列表
     * @param invTypes
     * @return
     */
    List<InvTypesPO> domain2Po(List<InvTypes> invTypes);

    default Byte map(Boolean value) {
        if (value) {
            return 1;
        }else {
            return 0;
        }
    }
}
