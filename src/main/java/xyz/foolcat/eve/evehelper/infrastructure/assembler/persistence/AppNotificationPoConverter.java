package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AppNotificationPO;

import java.util.List;

/**
 * AppNotification 领域实体与 AppNotificationPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 * date 2026-11-12
 */
@Mapper(componentModel = "spring")
public interface AppNotificationPoConverter {

    /**
     * AppNotificationPO 转换为 AppNotification
     * @param appNotificationPO 持久化对象
     * @return 领域实体
     */
    AppNotification po2Domain(AppNotificationPO appNotificationPO);

    /**
     * AppNotification 转换为 AppNotificationPO
     * @param appNotification 领域实体
     * @return 持久化对象
     */
    AppNotificationPO domain2Po(AppNotification appNotification);

    /**
     * AppNotificationPO 列表转换为 AppNotification 列表
     * @param appNotificationPO 持久化对象列表
     * @return 领域实体列表
     */
    List<AppNotification> po2Domain(List<AppNotificationPO> appNotificationPO);

    /**
     * AppNotification 列表转换为 AppNotificationPO 列表
     * @param appNotification 领域实体列表
     * @return 持久化对象列表
     */
    List<AppNotificationPO> domain2Po(List<AppNotification> appNotification);
}
