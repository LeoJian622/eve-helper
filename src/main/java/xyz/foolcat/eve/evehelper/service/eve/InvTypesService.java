package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import xyz.foolcat.eve.evehelper.domain.eve.InvTypes;
import xyz.foolcat.eve.evehelper.mapper.eve.InvTypesMapper;

@Service
public class InvTypesService {

    @Resource
    private InvTypesMapper invTypesMapper;


    public int deleteByPrimaryKey(Integer typeid) {
        return invTypesMapper.deleteByPrimaryKey(typeid);
    }


    public int insert(InvTypes record) {
        return invTypesMapper.insert(record);
    }


    public int insertSelective(InvTypes record) {
        return invTypesMapper.insertSelective(record);
    }


    public InvTypes selectByPrimaryKey(Integer typeid) {
        return invTypesMapper.selectByPrimaryKey(typeid);
    }

    public Integer selectIdByTypeName(String name) {
        return invTypesMapper.selectIdByTypeName(name);
    }


    public int updateByPrimaryKeySelective(InvTypes record) {
        return invTypesMapper.updateByPrimaryKeySelective(record);
    }


    public int updateByPrimaryKey(InvTypes record) {
        return invTypesMapper.updateByPrimaryKey(record);
    }


    public int updateBatch(List<InvTypes> list) {
        return invTypesMapper.updateBatch(list);
    }

}
