package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CalendarApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CalendarEventAttendeesResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CalendarEventResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CalendarResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Calendar Api Test")
class CalendarApiTest {


    @Autowired
    CalendarApi calendarApi;

    @Autowired
    AuthorizeUtil authorizeUtil;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = authorizeUtil.authorize( 2112818290);
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryCharactersCalendar() {
        List<CalendarResponse> calendarResponses = calendarApi.queryCharactersCalendar(2112818290, "serenity", null, at).collectList().block();
        System.out.println("calendarResponses = " + calendarResponses);
    }

    @Test
    void queryCharactersCalendarEventId() {
        CalendarEventResponse calendarEventResponse = calendarApi.queryCharactersCalendarEventId(2112818290, "serenity", 604077, at).block();
        System.out.println("eventResponse = " + calendarEventResponse);
    }

    @Test
    void updateCharactersCalendarEventId() {
        Object o = calendarApi.updateCharactersCalendarEventId(2112818290, "serenity", 604077, xyz.foolcat.eve.evehelper.esi.enums.CalendarEventEnum.ACCEPTED, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void queryCharactersCalendarEventIdAttendees() {
        List<CalendarEventAttendeesResponse> calendarEventAttendeesResponses = calendarApi.queryCharactersCalendarEventIdAttendees(2112818290, "serenity", 604077, at).collectList().block();
        System.out.println("calendarEventAttendeesResponses = " + calendarEventAttendeesResponses);
    }
}