package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.esi.model.send.NewLabel;
import xyz.foolcat.eve.evehelper.esi.model.send.NewMail;
import xyz.foolcat.eve.evehelper.esi.model.sub.Recipient;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Mail Api Test")
class MailApiTest {

    @Autowired
    MailApi mailApi;

    @Autowired
    EveAccountService eveAccountService;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = eveAccountService.lambdaQuery().eq(EveAccount::getId, 3).one();
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryCharacterMails() {
        List<RequestedMailResponse> requestedMailResponses = mailApi.queryCharacterMails(2112818290, "serenity", null, null, at).collectList().block();
        System.out.println("requestedMailResponses = " + requestedMailResponses);
    }

    @Test
    void addCharacterMail() {
        NewMail newMail = new NewMail();
        newMail.setSubject("test");
        newMail.setBody("新邮件测试");

        Recipient recipient = new Recipient();
        recipient.setRecipientId(2112818290);
        recipient.setRecipientType("character");
        newMail.setRecipients(List.of(recipient));

        Integer integer = mailApi.addCharacterMail(2112818290, "serenity", newMail, at).block();
        System.out.println("integer = " + integer);
    }

    @Test
    void deleteCharacterMail() {
        Object o = mailApi.deleteCharacterMail(2112818290, "serenity", 79794658, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void queryCharacterMail() {
        MailResponse mailResponse = mailApi.queryCharacterMail(2112818290, "serenity", 79718162, at).block();
        System.out.println("mailResponse = " + mailResponse);
    }

    @Test
    void updateCharacterMail() {
        Object o = mailApi.updateCharacterMail(2112818290, "serenity", 79794658, List.of(), true, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void queryCharacterMailLabels() {
        MailLabelsAndUnreadCountsResponse mailLabelsAndUnreadCountsResponse = mailApi.queryCharacterMailLabels(2112818290, "serenity", at).block();
        System.out.println("mailLabelsAndUnreadCountsResponse = " + mailLabelsAndUnreadCountsResponse);
    }

    @Test
    void addCharacterMailLabels() {
        NewLabel newLabel = new NewLabel();
        newLabel.setName("test");
        Integer integer = mailApi.addCharacterMailLabels(2112818290, "serenity", newLabel, at).block();
        System.out.println("integer = " + integer);
    }

    @Test
    void deleteCharacterMailLabel() {
        Integer integer = mailApi.deleteCharacterMailLabel(2112818290, "serenity", 32, at).block();
        System.out.println("integer = " + integer);
    }

    @Test
    void queryCharacterMailList() {
        List<MailListResponse> mailListResponses = mailApi.queryCharacterMailList(2112818290, "serenity", at).collectList().block();
        System.out.println("mailListResponses = " + mailListResponses);
    }
}