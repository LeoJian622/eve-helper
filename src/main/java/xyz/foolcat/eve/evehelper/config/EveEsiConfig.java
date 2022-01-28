package xyz.foolcat.eve.evehelper.config;

import com.dtflys.forest.annotation.BindingVar;
import com.dtflys.forest.reflection.ForestMethod;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * esi配置文件
 *
 * @author Leojan
 * @date 2021-12-03 15:15
 */

@Component
@ConfigurationProperties(prefix = "eve.esi")
public class EveEsiConfig {


//    @BindingVar("Authorization")
//    public String getAuthorization(ForestMethod method){
//        String methodName = method.getMethodName();
//        if (methodName.equals("getData")) {
//            // 若调用的是 getData 方法，则返回 192.168.0.2
//            return "192.168.0.2";
//        }
//        // 默认返回 192.168.0.1
//        return "192.168.0.1";
//    }


}
