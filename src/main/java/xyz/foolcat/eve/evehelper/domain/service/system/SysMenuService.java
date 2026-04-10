package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysMenu;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysMenuRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class SysMenuService{

    private final SysMenuRepository sysMenuRepository;

    public int updateBatch(List<SysMenu> list) {
        return sysMenuRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<SysMenu> list) {
        return sysMenuRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<SysMenu> list) {
        return sysMenuRepository.batchInsert(list);
    }

    public boolean insertOrUpdate(SysMenu record) {
        return sysMenuRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysMenu record) {
        return sysMenuRepository.insertOrUpdateSelective(record);
    }
}

