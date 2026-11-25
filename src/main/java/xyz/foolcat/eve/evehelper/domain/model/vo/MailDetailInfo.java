package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 邮件详情(领域读模型)。
 *
 * @param mailId     邮件 ID
 * @param from       发件人 ID
 * @param recipients 收件人列表
 * @param subject    主题
 * @param timestamp  发送时间
 * @param body       正文(HTML)
 * @param isRead     是否已读
 * @author Leojan
 * @since 022
 */
public record MailDetailInfo(Integer mailId, Integer from, List<MailRecipient> recipients,
                             String subject, OffsetDateTime timestamp, String body, Boolean isRead) {
}
