package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtflys.forest.exceptions.ForestNetworkException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.mapper.system.BlueprintsMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.vo.BlueprintsVO;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BlueprintsService extends ServiceImpl<BlueprintsMapper, Blueprints> {

    @Resource
    private EsiApiService esiApiService;


    public int batchInsert(List<Blueprints> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 与ESI蓝图数据进行同步并返回
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public void saveAndUpdateBlueprints(String type, String id) throws ParseException {
        List<Blueprints> blueprintsList;
        int page = 1;
        do {
            try {
                blueprintsList = esiApiService.getBlueprintsList(type, page, id);
            } catch (ForestNetworkException e) {
                if (e.getMessage().indexOf("Requested page does not exist") > 0) {
                    break;
                } else {
                    log.error("ESI请求失败", e);
                    break;
                }
            }
            Long ownId = Long.valueOf(id);
            blueprintsList.forEach(assets -> {
                assets.setOwnerId(ownId);
            });
            this.saveOrUpdateBatch(blueprintsList);
            page++;
        } while (blueprintsList.size() != 0);
    }

    /**
     * 获取蓝图列表
     *
     * @param id
     * @return
     */
    public IPage<BlueprintsVO> getBlueprintsListById(IPage<BlueprintsVO> page, String id) {
        return baseMapper.selectBlueprintsInvtypeUniverse(page, id);
    }

    public int updateBatch(List<Blueprints> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Blueprints> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int insertOrUpdate(Blueprints record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Blueprints record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

