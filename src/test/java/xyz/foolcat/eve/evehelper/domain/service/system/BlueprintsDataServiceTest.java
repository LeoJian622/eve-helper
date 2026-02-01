package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.domain.service.system.BlueprintsDataService;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("蓝图基础花费")
class BlueprintsDataServiceTest {

    @Autowired
    BlueprintsDataService blueprintsDataService;

    @Test
    void queryAllBlueprintsCost() {
        List<BlueprintCostDTO> result = blueprintsDataService.queryAllBlueprintsCost();
        Assert.notEmpty(result);
        System.out.println(result);
    }

    @Test
    void queryAllBlueprintsCostByBlueTypeId() {
        BlueprintCostDTO blueprintCostDTO = blueprintsDataService.queryAllBlueprintsCostByBlueTypeId(488);
        Assert.notNull(blueprintCostDTO);
        System.out.println(blueprintCostDTO);
    }
}