package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.crypto.digest.MD5;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yongj
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class MiningDetailService  {

    private final EsiGateway esiApiService;

    private final UniverseNameService universeNameService;

    private final AuthorizeUtil authorizeUtil;

    private final MiningDetailRepository miningDetailRepository;

    public int batchInsert(List<MiningDetail> list) {
        return miningDetailRepository.batchInsert(list);
    }

    public void saveObserverMining(Integer characterId, Long observerId) throws ParseException {
        EveAccount eveAccount = authorizeUtil.authorize(characterId);
        String accessToken = esiApiService.getAccessToken(characterId, eveAccount.getUserId());

        Integer maxPage = esiApiService.queryCorporationMiningObserverMaxPage(eveAccount.getCorpId(), observerId, accessToken);

        List<MiningDetail> miningDetails = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> esiApiService.queryCorporationMiningObserver(eveAccount.getCorpId(), observerId, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<Integer> items = new ArrayList<>();
        items.addAll(miningDetails.stream().map(MiningDetail::getCharacterId).distinct().collect(Collectors.toList()));
        items.addAll(miningDetails.stream().map(MiningDetail::getRecordedCorporationId).distinct().collect(Collectors.toList()));
        Map<Integer, String> universeName = universeNameService.getUniverseName(items);

        MD5 md5 = new MD5();

        // US1(014 T007):同步者 user_id 随写路径落库 —— 采掘数据只对当前同步用户可见的归属依据
        Long syncUserId = eveAccount.getUserId() == null ? null : eveAccount.getUserId().longValue();
        miningDetails.forEach(miningDetail -> {
            // 用哈希生成主键原因：
            // 将角色、矿石类型、建筑、采集时间四个业务维度拼接后取 MD5 前 16 位作为记录主键，
            // 可得到确定性的稳定 id——同一笔采集数据无论重复同步多少次主键都不变，
            // 配合 saveOrUpdateBatch 实现幂等写入，避免同一采集记录被重复插入（天然防重）。
            long key = miningDetail.getCharacterId() + miningDetail.getTypeId() + observerId + miningDetail.getLastUpdated().getTime();
            String md5Digest = md5.digestHex16(Long.toString(key), StandardCharsets.UTF_8);
            miningDetail.setId(md5Digest);
            miningDetail.setObserverId(observerId);
            miningDetail.setUserId(syncUserId);
            miningDetail.setCharacterName(universeName.get(miningDetail.getCharacterId()));
            miningDetail.setRecordedCorporationName(universeName.get(miningDetail.getRecordedCorporationId()));
        });

        try {
            miningDetailRepository.saveOrUpdateBatch(miningDetails);
        } catch (EveHelperException e) {
            log.warn("MiningDetais记录长度为0, 错误: {}", e.getMessage(), e);
        }

    }

    public int updateBatch(List<MiningDetail> list) {
        return miningDetailRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<MiningDetail> list) {
        return miningDetailRepository.updateBatchSelective(list);
    }

    public boolean insertOrUpdate(MiningDetail record) {
        return miningDetailRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(MiningDetail record) {
        return miningDetailRepository.insertOrUpdateSelective(record);
    }
}




