package xyz.foolcat.eve.evehelper.interceptor;

import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import com.dtflys.forest.reflection.ForestMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * @author Leojan
 * @date 2021-12-09 16:05
 */

@Component
@Slf4j
public class EsiClentInterceptor<T> implements Interceptor<T> {

    private final static Pattern PATTERN_LOGIN = Pattern.compile("^https://login.evepc.163.com");


    /**
     * 该方法在被调用时，并在beforeExecute前被调用
     * @Param request Forest请求对象
     * @Param args 方法被调用时传入的参数数组
     */
    @Override
    public void onInvokeMethod(ForestRequest req, ForestMethod method, Object[] args) {

        log.debug("add datasource and language");
        String url = req.getUrl();
        if (!PATTERN_LOGIN.matcher(url).find()){
            if (url.indexOf("?") > 0){
                url += "&datasource=serenity&language=zh";
            }else {
                url += "?datasource=serenity&language=zh";
            }
            req.setUrl(url);

        }
    }

    /**
     * 设置请求的access_token
     * @param req
     */
    private void setAccessToken(ForestRequest req){
    }

    /**
     * 该方法在请求发送之前被调用, 若返回false则不会继续发送请求
     * @Param request Forest请求对象
     */
    @Override
    public boolean beforeExecute(ForestRequest req) {

        return true;
    }

    /**
     * 该方法在请求成功响应时被调用
     */
    @Override
    public void onSuccess(T data, ForestRequest req, ForestResponse res) {
        //执行请求成功后代码
    }

    /**
     * 该方法在请求发送失败时被调用
     */
    @Override
    public void onError(ForestRuntimeException ex, ForestRequest req, ForestResponse res) {
        // 执行发送请求失败后处理的代码
    }

    /**
     * 该方法在请求发送之后被调用
     */
    @Override
    public void afterExecute(ForestRequest req, ForestResponse res) {
        // 执行在发送请求之后处理的代码

    }
}
