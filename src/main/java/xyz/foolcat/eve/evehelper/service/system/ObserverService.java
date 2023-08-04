package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtflys.forest.exceptions.ForestNetworkException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.system.Observer;
import xyz.foolcat.eve.evehelper.mapper.system.ObserverMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategyContext;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class ObserverService extends ServiceImpl<ObserverMapper, Observer> {

    private final EsiApiService esiApiService;

    private final EsiClientStrategyContext esiClientStrategyContext;

    public int updateBatch(List<Observer> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Observer> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<Observer> list) {
        return baseMapper.batchInsert(list);
    }

    public void saveObserverFromEsi(Long corporationId) throws ParseException {
        String accessToken = esiApiService.getAccessToken(corporationId.toString());
        List<Observer> observerList = new ArrayList<>();
        int i = 1;
        while (true) {
            try {
                List<Observer> temp = esiClientStrategyContext.getResource(GlobalConstants.CROP).getCropObserverList(corporationId, i++, accessToken);
                observerList.addAll(temp);
            } catch (ForestNetworkException e) {
                break;
            }
        }
        observerList.forEach(observer -> observer.setCroporationId(corporationId));
        saveOrUpdateBatch(observerList);
    }

    public int insertOrUpdate(Observer record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Observer record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

