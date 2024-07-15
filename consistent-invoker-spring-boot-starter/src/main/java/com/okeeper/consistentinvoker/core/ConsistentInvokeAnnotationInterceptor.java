package com.okeeper.consistentinvoker.core;

import com.okeeper.consistentinvoker.utils.SpelUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;


/**
 * @author zhangyue
 * @AsyncInvoke 注解拦截
 */
public class ConsistentInvokeAnnotationInterceptor implements MethodInterceptor {

    private ConsistentInvokeHandler consistentInvokeHandler;

    public ConsistentInvokeAnnotationInterceptor(ConsistentInvokeHandler consistentInvokeHandler) {
        this.consistentInvokeHandler = consistentInvokeHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable{
        Method method = invocation.getMethod();
        //获取注解属性
        ConsistentInvoke consistentInvoke = method.getAnnotation(ConsistentInvoke.class);
        //获取通过springEl表达式获取方法入参的关键字值
        String keyValue = SpelUtils.getMethodParameterValue(method, invocation.getArguments(), consistentInvoke.key());
        return consistentInvokeHandler.invoke(
                invocation,
                method,
                invocation.getArguments(),
                consistentInvoke.maxRetryCount(),
                keyValue,
                consistentInvoke.async());
    }
}