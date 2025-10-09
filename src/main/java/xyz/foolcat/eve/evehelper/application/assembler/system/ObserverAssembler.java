package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.ObserverPO;

import java.util.List;

;

/**
 * 观察者转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface ObserverAssembler {

    /**
     * ObserverPO 转换为 Observer
     * @param observerPO
     * @return
     */
    Observer po2Domain(ObserverPO observerPO);

    /**
     * Observer 转换为 ObserverPO
     * @param observer
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    ObserverPO domain2Po(Observer observer);

    /**
     * ObserverPO 转换为 Observer
     * @param observerPO
     * @return
     */
    List<Observer> po2Domain(List<ObserverPO> observerPO);

    /**
     * Observer 转换为 ObserverPO
     * @param observer
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    List<ObserverPO> domain2Po(List<Observer> observer);
} 