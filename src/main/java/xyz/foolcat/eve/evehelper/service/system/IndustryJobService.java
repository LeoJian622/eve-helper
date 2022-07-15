package xyz.foolcat.eve.evehelper.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.system.IndustryJob;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.mapper.system.IndustryJobMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class IndustryJobService extends ServiceImpl<IndustryJobMapper, IndustryJob> {

    private final EsiApiService esiApiService;

    public int updateBatch(List<IndustryJob> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<IndustryJob> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<IndustryJob> list) {
        return baseMapper.batchInsert(list);
    }

    public List<IndustryJob> queryCharJobs(String id) throws ParseException {
        List<IndustryJobDTO> jobs = esiApiService.getJobList(GlobalConstants.CHAR, id);
//        jobs.stream().map()

        return null;
    }
}



