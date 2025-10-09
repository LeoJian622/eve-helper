package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysPermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysPermissionRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class SysPermissionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysPermissionRepository rolePermissionRepository;

    public int updateBatch(List<SysPermission> list) {
        return rolePermissionRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<SysPermission> list) {
        return rolePermissionRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<SysPermission> list) {
        return rolePermissionRepository.batchInsert(list);
    }

    public void refreshPermRolesRules() {
        redisTemplate.delete(List.of(GlobalConstants.URL_PERM_ROLES_KEY, GlobalConstants.BTN_PERM_ROLES_KEY));
        List<SysPermission> permissions = this.listPermRoles();
        if (CollectionUtil.isNotEmpty(permissions)) {
            //init URL【权限->角色（合集）】规则
            List<SysPermission> urlPermList = permissions.stream()
                    .filter(item -> StrUtil.isNotEmpty(item.getUrlPerm()))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(urlPermList)) {
                Map<String, List<String>> urlPermRoles = new HashMap<>();
                urlPermList.forEach(item -> {
                    String perm = item.getUrlPerm();
                    List<String> roles = item.getRoles();
                    urlPermRoles.put(perm, roles);
                });
                redisTemplate.opsForHash().putAll(GlobalConstants.URL_PERM_ROLES_KEY, urlPermRoles);

                redisTemplate.convertAndSend("cleanRoleLocalCache", "true");
            }

            //init URL【按钮->角色（合集）】规则
            List<SysPermission> btnPermList = permissions.stream()
                    .filter(item -> StrUtil.isNotEmpty(item.getBtnPerm()))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(btnPermList)) {
                Map<String, List<String>> btnRoles = new HashMap<>();
                btnPermList.forEach(item -> {
                    String perm = item.getBtnPerm();
                    List<String> roles = item.getRoles();
                    btnRoles.put(perm, roles);
                });

                redisTemplate.opsForHash().putAll(GlobalConstants.BTN_PERM_ROLES_KEY, btnRoles);

                redisTemplate.convertAndSend("cleanRoleLocalCache", "true");
            }

        }
    }

    private List<SysPermission> listPermRoles() {
        return rolePermissionRepository.listPermRoles();
    }

    public int insertOrUpdate(SysPermission record) {
        return rolePermissionRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysPermission record) {
        return rolePermissionRepository.insertOrUpdateSelective(record);
    }
}

