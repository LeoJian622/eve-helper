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
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.esi.model.BookmarkFoldersResponse;
import xyz.foolcat.eve.evehelper.esi.model.BookmarksResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Bookmarks Api Test")
class BookmarksApiTest {

    @Autowired
    BookmarksApi bookmarksApi;

    @Autowired
    EveAccountService eveAccountService;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, 2112818290).one();
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryCharactersBookmarks() {
        List<BookmarksResponse> bookmarksResponseList = bookmarksApi.queryCharactersBookmarks(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("bookmarksResponseList = " + bookmarksResponseList);
    }

    @Test
    void queryCharactersBookmarksFolders() {
        List<BookmarkFoldersResponse> bookmarkFoldersResponseList = bookmarksApi.queryCharactersBookmarksFolders(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("bookmarkFoldersResponseList = " + bookmarkFoldersResponseList);
    }

    @Test
    void queryCorporationsBookmarks() {
        List<BookmarksResponse> bookmarksResponseList = bookmarksApi.queryCorporationsBookmarks(656880659, "serenity", 1, at).collectList().block();
        System.out.println("bookmarksResponseList = " + bookmarksResponseList);
    }

    @Test
    void queryCorporationsBookmarksFolders() {
        List<BookmarkFoldersResponse> bookmarkFoldersResponseList = bookmarksApi.queryCorporationsBookmarksFolders(656880659, "serenity", 1, at).collectList().block();
        System.out.println("bookmarkFoldersResponseList = " + bookmarkFoldersResponseList);
    }
}