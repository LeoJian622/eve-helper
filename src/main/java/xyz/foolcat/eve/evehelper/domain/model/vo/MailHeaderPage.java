package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 邮件标题分页结果(领域读模型)。
 * <p>
 * 包含一页邮件标题及下一页游标。{@code nextLastMailId} 为 null 表示无更多页。
 *
 * @param mails          本页邮件标题列表
 * @param nextLastMailId 下一页游标(本页最后一条 mailId);不足一页时为 null
 * @author Leojan
 * @since 022
 */
public record MailHeaderPage(List<MailHeaderItem> mails, Integer nextLastMailId) {

    /**
     * 单条邮件标题。
     *
     * @param mailId    邮件 ID
     * @param from      发件人 ID
     * @param subject   主题
     * @param timestamp 发送时间
     * @param isRead    是否已读
     * @param labels    标签 ID 列表
     */
    public record MailHeaderItem(Integer mailId, Integer from, String subject,
                                 OffsetDateTime timestamp, Boolean isRead, List<Integer> labels) {
    }
}
