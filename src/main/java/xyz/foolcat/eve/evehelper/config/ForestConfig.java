package xyz.foolcat.eve.evehelper.config;

import com.dtflys.forest.converter.json.ForestJacksonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forest 配置
 *
 * @author Leojan
 * date 2022-04-19 9:15
 */

@Configuration
public class ForestConfig {

    @Bean
    ForestJacksonConverter forestJacksonConverter(){
        ForestJacksonConverter forestJacksonConverter = new ForestJacksonConverter();
        ObjectMapper objectMapper = forestJacksonConverter.getMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return forestJacksonConverter;
    }
}
