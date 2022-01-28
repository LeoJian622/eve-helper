package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import xyz.foolcat.eve.evehelper.mapper.eve.IndustryActivityProductsMapper;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryActivityProducts;
@Service
public class IndustryActivityProductsService{

    @Resource
    private IndustryActivityProductsMapper industryActivityProductsMapper;

    
    public int insert(IndustryActivityProducts record) {
        return industryActivityProductsMapper.insert(record);
    }

    
    public int insertSelective(IndustryActivityProducts record) {
        return industryActivityProductsMapper.insertSelective(record);
    }

}
