package xyz.foolcat.eve.evehelper.service.system;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtflys.forest.exceptions.ForestNetworkException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.system.MiningDetail;
import xyz.foolcat.eve.evehelper.mapper.system.MiningDetailMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategyContext;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class MiningDetailService extends ServiceImpl<MiningDetailMapper, MiningDetail> {

    private final EsiApiService esiApiService;

    private final EsiClientStrategyContext esiClientStrategyContext;

    private final UniverseNameService universeNameService;

    public int batchInsert(List<MiningDetail> list) {
        return baseMapper.batchInsert(list);
    }

    public void saveObserverMining(Long corporationId, Long observerId) throws ParseException {
        String accessToken = esiApiService.getAccessToken(GlobalConstants.CHAR, corporationId.toString());
        int i = 1;
        List<MiningDetail> miningDetails = new ArrayList<>();
        while (true) {
            try {
                List<MiningDetail> temp = esiClientStrategyContext.getResource(GlobalConstants.CROP).getMiningDetailListByObserver(corporationId, observerId, i++, accessToken);
                miningDetails.addAll(temp);
            } catch (ForestNetworkException e) {
                break;
            }
        }

        List<Integer> items = new ArrayList<>();
        items.addAll(miningDetails.stream().map(MiningDetail::getCharacterId).distinct().collect(Collectors.toList()));
        items.addAll(miningDetails.stream().map(MiningDetail::getRecordedCorporationId).distinct().collect(Collectors.toList()));
        Map<Integer, String> universeName = universeNameService.getUniverseName(items);

        MD5 md5 = new MD5();

        miningDetails.forEach(miningDetail -> {
            long key = miningDetail.getCharacterId() + miningDetail.getTypeId() + observerId + miningDetail.getLastUpdated().getTime();
            String md5Digest = md5.digestHex16(Long.toString(key), StandardCharsets.UTF_8);
            miningDetail.setId(md5Digest);
            miningDetail.setObserverId(observerId);
            miningDetail.setCharacterName(universeName.get(miningDetail.getCharacterId()));
            miningDetail.setRecordedCorporationName(universeName.get(miningDetail.getRecordedCorporationId()));
        });
        saveOrUpdateBatch(miningDetails);
    }

    public int updateBatch(List<MiningDetail> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<MiningDetail> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int insertOrUpdate(MiningDetail record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(MiningDetail record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}




