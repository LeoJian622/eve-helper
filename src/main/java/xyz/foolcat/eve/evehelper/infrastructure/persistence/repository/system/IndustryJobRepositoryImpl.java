package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.repository.system.IndustryJobRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.IndustryJobPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.IndustryJobMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class IndustryJobRepositoryImpl implements IndustryJobRepository {

    private final IndustryJobMapper industryJobMapper;
    private final IndustryJobPoConverter industryJobPoConverter;

    @Override
    public int updateBatch(List<IndustryJob> list) {
        return industryJobMapper.updateBatch(industryJobPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<IndustryJob> list) {
        return industryJobMapper.updateBatchSelective(industryJobPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<IndustryJob> list) {
        return industryJobMapper.batchInsert(industryJobPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(IndustryJob record) {
        return industryJobMapper.insertOrUpdate(industryJobPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(IndustryJob record) {
        return industryJobMapper.insertOrUpdateSelective(industryJobPoConverter.domain2Po(record));
    }

    @Override
    public int batchInsertOrUpdate(List<IndustryJob> list) {
        return industryJobMapper.batchInsertOrUpdate(industryJobPoConverter.domain2Po(list));
    }

    @Override
    public List<IndustryJob> selectByCorpIdAndStatus(Integer corpId, String statusDelivered) {
        List<IndustryJobPO> industryJobPOS = industryJobMapper.selectList(new QueryWrapper<IndustryJobPO>().lambda().eq(IndustryJobPO::getCorporationId, corpId)
                .ne(IndustryJobPO::getStatus, statusDelivered));
        return industryJobPoConverter.po2Domain(industryJobPOS);
    }
}
