package xyz.foolcat.eve.evehelper.service.eve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.service.system.InvTypesService;



@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("typeId 物品信息读取")
class InvTypesServiceTest {

    @Autowired
    InvTypesService invTypesService;

    @Test
    void selectByPrimaryKey() {
        System.out.println(invTypesService.lambdaQuery().eq(InvTypes::getTypeId,50).one());
    }

    @Test
    void selectIdByTypeName() {
        System.out.println(invTypesService.lambdaQuery().eq(InvTypes::getName,"毒蜥级").one());
    }

    @Test
    void updateBatch() {
    }

    @Test
    void updateBatchSelective() {
    }

    @Test
    void batchInsert() {
    }

    @Test
    void insertOrUpdate() {
    }

    @Test
    void insertOrUpdateSelective() {
    }

    @Test
    void getNameByTypeIds() {
    }

    @Test
    void updateTypeByTypeId() {
        invTypesService.updateTypeByTypeId(75993);
    }
}