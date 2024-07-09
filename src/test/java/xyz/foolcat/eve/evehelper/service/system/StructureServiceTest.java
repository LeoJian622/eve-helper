package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.system.Structure;

import java.text.ParseException;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("建筑相关服务")
class StructureServiceTest {

    @Autowired
    StructureService structureService;

    @Test
    void batchInsertOrUpdateFromEsi() throws ParseException {
        structureService.batchInsertOrUpdateFromEsi(2112818290);
    }

    @Test
    void selectFuelExpiresList() {
        List<Structure> structures = structureService.selectFuelExpiresList(48, 656880659);
        System.out.println("structures = " + structures);
    }
}