package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 邮件收件人(领域读模型)。
 * <p>
 * T016 创建(邮件详情用),T017 SendMailCommand 复用。
 *
 * @param recipientId   收件人 ID
 * @param recipientType 收件人类型(character/corporation/alliance/mailing_list)
 * @author Leojan
 * @since 022
 */
public record MailRecipient(Integer recipientId, String recipientType) {
}
