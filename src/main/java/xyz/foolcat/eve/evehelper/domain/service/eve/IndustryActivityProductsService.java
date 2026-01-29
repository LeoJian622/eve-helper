package xyz.foolcat.eve.evehelper.domain.service.eve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityProducts;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryActivityProductsRepository;

/**
 * @author yongj
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class IndustryActivityProductsService {

    private final IndustryActivityProductsRepository industryActivityProductsRepository;


    public int insert(IndustryActivityProducts record) {
        return industryActivityProductsRepository.insert(record);
    }


    public int insertSelective(IndustryActivityProducts record) {
        return industryActivityProductsRepository.insertSelective(record);
    }

}
