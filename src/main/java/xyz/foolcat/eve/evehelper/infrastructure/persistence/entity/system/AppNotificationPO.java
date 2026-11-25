package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * APP 通知持久化对象
 * <p>
 * 对应数据库表 app_notification,承载通知事件的持久化存储。
 *
 * @author Leojan
 * date 2026-11-12
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "APP 通知表")
@Data
@TableName(value = "app_notification")
public class AppNotificationPO extends BaseEntity implements Serializable {

    /**
     * 通知主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "通知主键ID")
    private Long id;

    /**
     * 接收通知的系统用户ID
     */
    @TableField(value = "user_id")
    @Schema(description = "接收通知的系统用户ID")
    private Integer userId;

    /**
     * 事件类型(枚举 .name() 存储)
     */
    @TableField(value = "event_type")
    @Schema(description = "事件类型")
    private String eventType;

    /**
     * 幂等键(防重复推送)
     */
    @TableField(value = "dedup_key")
    @Schema(description = "幂等键")
    private String dedupKey;

    /**
     * 关联角色ID(可空)
     */
    @TableField(value = "character_id")
    @Schema(description = "关联角色ID")
    private Integer characterId;

    /**
     * 关联军团ID(可空)
     */
    @TableField(value = "corp_id")
    @Schema(description = "关联军团ID")
    private Integer corpId;

    /**
     * 通知标题
     */
    @TableField(value = "title")
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知正文
     */
    @TableField(value = "content")
    @Schema(description = "通知正文")
    private String content;

    /**
     * 是否已读
     */
    @TableField(value = "is_read")
    @Schema(description = "是否已读")
    private Boolean isRead;

    @Serial
    private static final long serialVersionUID = 1L;
}
