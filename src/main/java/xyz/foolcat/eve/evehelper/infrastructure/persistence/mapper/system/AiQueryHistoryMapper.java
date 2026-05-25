package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.AiQueryHistoryPO;

import java.util.List;

/**
 * AI查询历史 Mapper
 * 使用 system 数据源
 */
public interface AiQueryHistoryMapper extends BaseMapper<AiQueryHistoryPO> {

    /**
     * 查询用户的查询历史
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return 查询历史列表
     */
    List<AiQueryHistoryPO> findByUserId(@Param("userId") Integer userId, @Param("limit") int limit);
}
