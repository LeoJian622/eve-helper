package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.mapper.eve.IndustryActivityMaterialsMapper;
import xyz.foolcat.eve.evehelper.mapper.system.InvTypesMapper;

import javax.annotation.Resource;

/**
 * EVE工业制造计算服务
 *
 * @author Leojan
 * date 2021-12-06 16:55
 */

@Service
public class IndustryService {

    @Resource
    private InvTypesMapper invTypesMapper;

    @Resource
    private IndustryActivityMaterialsMapper industryActivityMaterialsMapper;

}
