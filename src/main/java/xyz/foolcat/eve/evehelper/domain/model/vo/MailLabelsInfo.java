package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 邮件标签与未读数(领域读模型)。
 *
 * @param labels     标签列表
 * @param totalUnread 总未读数
 * @author Leojan
 * @since 022
 */
public record MailLabelsInfo(List<MailLabel> labels, Long totalUnread) {

    /**
     * 单个邮件标签。
     *
     * @param labelId     标签 ID
     * @param name        标签名称
     * @param unreadCount 该标签下未读数
     */
    public record MailLabel(Integer labelId, String name, Long unreadCount) {
    }
}
