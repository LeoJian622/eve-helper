package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Route Api Test")
class RouteApiTest {

    @Autowired
    RouteApi routeApi;

    @Test
    void queryUniverseSchematic() {
        List<Integer> integers1 = new ArrayList<>();
        integers1.add(30003571);
        integers1.add(30004236);
        List<List<Integer>> a = new ArrayList<>();
        a.add(integers1);
        List<Integer> integers = routeApi.queryUniverseSchematic(30003573, 30004245, null, a, "serenity").collectList().block();
        System.out.println("integers = " + integers);
    }
}