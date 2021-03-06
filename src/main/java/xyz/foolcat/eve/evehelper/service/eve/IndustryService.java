package xyz.foolcat.eve.evehelper.service.eve;

import com.fasterxml.jackson.databind.util.JSONPObject;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.eve.InvTypes;
import xyz.foolcat.eve.evehelper.mapper.eve.IndustryActivityMaterialsMapper;
import xyz.foolcat.eve.evehelper.mapper.eve.InvTypesMapper;

import javax.annotation.Resource;

/**
 * EVE工业制造计算服务
 *
 * @author Leojan
 * @date 2021-12-06 16:55
 */

@Service
public class IndustryService {

    @Resource
    private InvTypesMapper invTypesMapper;

    @Resource
    private IndustryActivityMaterialsMapper industryActivityMaterialsMapper;

    public JSONPObject queryMaterialNeedAndPrice(String name){
        Integer typeId = invTypesMapper.selectIdByTypeName(name);

        return null;
    }
}
