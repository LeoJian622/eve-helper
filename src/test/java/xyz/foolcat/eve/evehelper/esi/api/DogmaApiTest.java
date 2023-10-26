package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Corporation Api Test")
class DogmaApiTest {

    @Autowired
    DogmaApi dogmaApi;

}