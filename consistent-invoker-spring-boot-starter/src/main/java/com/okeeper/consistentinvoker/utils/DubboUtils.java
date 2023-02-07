package com.okeeper.consistentinvoker.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * dubbo 泛化调用工具类
 * @author yue
 */
@Slf4j
public class DubboUtils {

    private static DubboUtils instance;

    public static DubboUtils getInstance(ApplicationContext applicationContext) {
        if(instance == null) {
            synchronized (DubboUtils.class) {
                if(instance == null) {
                    instance = new DubboUtils(applicationContext);
                }
            }
        }
        return instance;
    }

    public String getDubboApplicationName() {
        String dubboApplicationName = applicationContext.getEnvironment().getProperty("dubbo.application.name");
        String applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        String appName = StringUtils.isEmpty(dubboApplicationName) ? applicationName : dubboApplicationName;
        if(StringUtils.isEmpty(appName)) {
            throw new IllegalArgumentException("'dubbo.application.name' or 'spring.application.name' is not configured.");
        }
        return appName;
    }

    private DubboUtils(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected ApplicationContext applicationContext;

    /**
     * 缓存远程方法定义
     */
    private ConcurrentHashMap<String, GenericService> cachedService = new ConcurrentHashMap<>();

    /**
     * 访问rpc
     * @param interfaceName
     * @param methodName
     * @param parameterTypes
     * @param request
     * @return
     */
    public Object invokeRpc(String targetAppName, String interfaceName, String methodName, String[] parameterTypes, Object request) {
        Object result = null;
        try {
            GenericService genericService = findRpcInterface(targetAppName, interfaceName);
            //调用泛化接口，返回单一元素
            result = genericService.$invoke(methodName, parameterTypes, new Object[]{request});
            log.info("[Consumer]==> invoke rpc {} success. request = 【{}】,response = 【{}】", interfaceName + "#" + interfaceName, JSON.toJSONString(request), JSON.toJSONString(result));
            return result;
        }catch (Throwable throwable) {
            log.warn("[Consumer]==> invoke rpc {} error. request = 【{}】, response = 【{}】", interfaceName + "#" + interfaceName, JSON.toJSONString(request), JSON.toJSONString(result) , throwable);
            throw new RpcException("invokeRpc fail.");
        }
    }


    private ApplicationConfig getApplicationConfig() {
        return applicationContext.getBean(ApplicationConfig.class);
    }

    private ProtocolConfig getProtocolConfig() {
        return applicationContext.getBean(ProtocolConfig.class);
    }

    private RegistryConfig getRegistryConfig() {
        return applicationContext.getBean(RegistryConfig.class);
    }


    /**
     * 订阅一个rpc接口并缓存
     * @param interfaceName 接口名称
     * @return GenericService
     */
    private GenericService findRpcInterface(String targetAppName, String interfaceName) {
        String path = targetAppName + "/" + interfaceName;
        GenericService genericService = cachedService.get(path);
        if(genericService == null) {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setApplication(getApplicationConfig());
            reference.setRegistry(getRegistryConfig());
            reference.setInterface(interfaceName);
            reference.setTimeout(3000);
            reference.setGeneric(true);
            reference.setGroup(targetAppName);
            cachedService.putIfAbsent(path, ReferenceConfigCache.getCache().get(reference));
            return cachedService.get(path);
        }else {
            return genericService;
        }
    }

    public <T> void exportInterface(Class<T> interfaceClass, T interfaceImplObject) {
        ServiceConfig<T> service = new ServiceConfig<>();
        service.setGroup(getDubboApplicationName());
        service.setInterface(interfaceClass);
        service.setRef(interfaceImplObject);
        service.setApplication(getApplicationConfig());
        service.setProtocol(getProtocolConfig());
        service.setRegistry(getRegistryConfig());
        service.export();
    }
}
