package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 发送邮件命令(领域值对象)。
 * <p>
 * T017 创建,复用 T016 的 {@link MailRecipient}。
 *
 * @param recipients 收件人列表(支持 character/corporation/alliance/mailing_list)
 * @param subject    邮件主题
 * @param body       邮件正文(HTML)
 * @author Leojan
 * @since 022
 */
public record SendMailCommand(List<MailRecipient> recipients, String subject, String body) {
}
