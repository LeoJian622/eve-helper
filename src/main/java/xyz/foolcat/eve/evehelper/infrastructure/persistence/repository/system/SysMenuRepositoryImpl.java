package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysMenu;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysMenuRepository;

import java.util.List;

/**
 * SysMenuRepository 空实现(死代码 stub)。
 *
 * <p>TODO(缺陷修复决议保留):{@link SysMenuService} 无任何上游调用,整条 SysMenu 持久化链
 * 从未被生产代码触发,故本实现所有方法返回空/0/false。按 YAGNI 不实现真实逻辑;
 * 若未来需要菜单持久化,应先在 controller 接入调用方再补齐,而非先行实现。
 *
 * @author yongj
 * date 2025-07-10 15:11
 */
@Repository
@RequiredArgsConstructor
public class SysMenuRepositoryImpl implements SysMenuRepository {

    @Override
    public int updateBatch(List<SysMenu> list) {
        return 0;
    }

    @Override
    public int updateBatchSelective(List<SysMenu> list) {
        return 0;
    }

    @Override
    public int batchInsert(List<SysMenu> list) {
        return 0;
    }

    @Override
    public boolean insertOrUpdate(SysMenu record) {
        return false;
    }

    @Override
    public int insertOrUpdateSelective(SysMenu record) {
        return 0;
    }

    @Override
    public int batchInsertOrUpdate(List<SysMenu> list) {
        return 0;
    }
}
