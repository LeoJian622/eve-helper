package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleRepository;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleRepository sysRoleRepository;

    public int updateBatch(List<SysRole> list) {
        return sysRoleRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<SysRole> list) {
        return sysRoleRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<SysRole> list) {
        return sysRoleRepository.batchInsert(list);
    }

    public List<String> queryRolesByUserId(Integer id) {
        return sysRoleRepository.queryRolesByUserId(id);
    }

    public int insertOrUpdate(SysRole record) {
        return sysRoleRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysRole record) {
        return sysRoleRepository.insertOrUpdateSelective(record);
    }
}

