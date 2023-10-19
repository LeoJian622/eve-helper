package xyz.foolcat.eve.evehelper.strategy.esi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ESI客户端调度实用类
 *
 * @author Leojan
 * @date 2022-04-21 17:04
 */

@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class EsiClientStrategyContext {

    @Resource
    private final Map<String, EsiClientStrategy> esiClientStrategyMap = new ConcurrentHashMap<>();

    public EsiClientStrategy getResource(String type){
        return esiClientStrategyMap.get(type);
    }
}
