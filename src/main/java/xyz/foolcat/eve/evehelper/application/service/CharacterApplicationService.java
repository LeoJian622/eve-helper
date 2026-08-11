package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

/**
 * 角色应用服务
 * 负责角色 ESI 授权绑定用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterApplicationService {

    private final EsiGateway esiApiService;

    /**
     * 使用授权 code 绑定角色到当前用户。
     *
     * @param code   授权 code
     * @param userId 用户 ID
     */
    public void authorizeCharacter(String code, Integer userId) {
        try {
            esiApiService.authorize(code, userId);
        } catch (ParseException e) {
            log.error("角色授权失败:  code={}",  code, e);
            throw new EveHelperException("角色授权失败", e);
        }
    }
}