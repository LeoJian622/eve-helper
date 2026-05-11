package xyz.foolcat.eve.evehelper.domain.agent;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.application.service.AgentApplicationService;

/**
 *
 * @author yongj
 * date 2026-04-13 11:39
 */

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("Agent")
public class AgentTest {

    @Autowired
    AgentApplicationService agentApplicationService;

    @Test
    public void simpleAgent() throws GraphRunnerException {
        String whatWeather = agentApplicationService.simpleAgent("几点，天气怎么样");
        System.out.println("whatWeather = " + whatWeather);
    }
}