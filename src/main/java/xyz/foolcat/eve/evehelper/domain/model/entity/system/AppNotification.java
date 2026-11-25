package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * APP 通知领域实体
 * <p>
 * 伴侣 APP 通知通道的核心领域对象,承载推送事件的业务属性。
 *
 * @author Leojan
 * date 2026-11-12
 */
@Data
public class AppNotification extends BaseEntity implements Serializable {

    /**
     * 通知主键ID
     */
    private Long id;

    /**
     * 接收通知的系统用户ID
     */
    private Integer userId;

    /**
     * 事件类型(枚举 .name() 存储)
     */
    private String eventType;

    /**
     * 幂等键(防重复推送)
     */
    private String dedupKey;

    /**
     * 关联角色ID(可空)
     */
    private Integer characterId;

    /**
     * 关联军团ID(可空)
     */
    private Integer corpId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知正文
     */
    private String content;

    /**
     * 是否已读
     */
    private Boolean isRead;

    @Serial
    private static final long serialVersionUID = 1L;
}
