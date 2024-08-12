package xyz.foolcat.eve.evehelper.service.system;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.converter.esi.MiningDetailConverter;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.system.MiningDetail;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.mapper.system.MiningDetailMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.util.AuthorizeUtil;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class MiningDetailService extends ServiceImpl<MiningDetailMapper, MiningDetail> {

    private final EsiApiService esiApiService;


    private final UniverseNameService universeNameService;

    private final IndustryApi industryApi;

    private final MiningDetailConverter miningDetailConverter;

    public int batchInsert(List<MiningDetail> list) {
        return baseMapper.batchInsert(list);
    }

    public void saveObserverMining(Integer characterId, Long observerId) throws ParseException {
        EveAccount eveAccount = AuthorizeUtil.authorize(characterId);
        String accessToken = esiApiService.getAccessToken(characterId, eveAccount.getUserId());

        Integer maxPage = industryApi.queryCorporationMiningObserverMaxPage(eveAccount.getCorpId(), observerId, EsiClient.SERENITY, accessToken);

        List<MiningDetail> miningDetails = Stream.iterate(1, i -> i++).limit(maxPage)
                .map(i -> industryApi.queryCorporationMiningObserver(eveAccount.getCorpId(), EsiClient.SERENITY, observerId, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(miningDetailConverter::toMiningDetail)
                .collect(Collectors.toList());

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




