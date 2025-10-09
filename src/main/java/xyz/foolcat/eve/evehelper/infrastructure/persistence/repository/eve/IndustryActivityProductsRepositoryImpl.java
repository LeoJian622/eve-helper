package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.IndustryActivityProductsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityProducts;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryActivityProductsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.IndustryActivityProductsMapper;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class IndustryActivityProductsRepositoryImpl implements IndustryActivityProductsRepository {

    private final IndustryActivityProductsMapper industryActivityProductsMapper;

    private final IndustryActivityProductsAssembler industryActivityProductsAssembler;

    @Override
    public int insert(IndustryActivityProducts record) {
        return industryActivityProductsMapper.insert(industryActivityProductsAssembler.entity2Po(record));
    }

    @Override
    public int insertSelective(IndustryActivityProducts record) {
        return industryActivityProductsMapper.insertSelective(industryActivityProductsAssembler.entity2Po(record));
    }

}