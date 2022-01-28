package xyz.foolcat.eve.evehelper.service.eve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("typeId 物品信息读取")
class InvTypesServiceTest {

    @Autowired
    InvTypesService invTypesService;

    @Test
    void selectByPrimaryKey() {
        System.out.println(invTypesService.selectByPrimaryKey(50));
    }

    @Test
    void selectIdByTypeName() {
        System.out.println(invTypesService.selectIdByTypeName("毒蜥级"));
    }
}