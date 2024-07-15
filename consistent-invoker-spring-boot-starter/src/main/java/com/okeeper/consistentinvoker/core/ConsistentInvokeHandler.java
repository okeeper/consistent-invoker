package com.okeeper.consistentinvoker.core;

import com.okeeper.consistentinvoker.common.ConsistentConstants;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.okeeper.consistentinvoker.core.service.ConsistentInvokeRecordService;
import com.okeeper.consistentinvoker.core.tx.ConsistentTransactionSynchronization;
import com.okeeper.consistentinvoker.rpc.RetryInvokeRpcService;
import com.okeeper.consistentinvoker.utils.ClassUtils;
import com.okeeper.consistentinvoker.utils.DubboUtils;
import com.alibaba.fastjson.JSON;
import com.okeeper.consistentinvoker.utils.TxUtils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 异步访问方法handler
 *
 * @author yue
 */
@Slf4j
public class ConsistentInvokeHandler {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ConsistentInvokeRecordService consistentInvokeRecordService;

    @Autowired
    private TxUtils txUtils;


    public Object invoke(MethodInvocation invocation, Method method, Object[] args, int maxRetryCount, String key, boolean async) throws Throwable {
        Object target = invocation.getThis();
        ConsistentInvokeRecord consistentInvokeRecord = consistentInvokeRecordService.buildAsyncInvokeRecord(
                DubboUtils.getInstance(applicationContext).getDubboApplicationName(),
                target.getClass(),
                method,
                args,
                maxRetryCount,
                key);

        //异步执行，等业务事物提交之后执行
        if(async) {
            //先保存在执行
            consistentInvokeRecordService.save(consistentInvokeRecord);

            //分布式事物同步
            ConsistentTransactionSynchronization synchronization = new ConsistentTransactionSynchronization(
                    //事务提交后直接调用
                    () -> directInvoke(consistentInvokeRecord.getId(), target, method, args),
                    //事务回滚后删除
                    () -> {
                        consistentInvokeRecordService.delete(consistentInvokeRecord.getId());
                        return null;
                    }
            );
            //注册事物同步管理器
            consistentInvokeRecordService.registerInvokeRecordWitBizTransaction(synchronization);
            return null;
        } else {
            //非异步时直接执行
            return processInvoke(consistentInvokeRecord, invocation, target, method, args);
        }
    }


    /**
     * 通过rpc访问目标应用的方法
     * @param consistentInvokeRecord
     */
    public void invokeByRpc(ConsistentInvokeRecord consistentInvokeRecord) {
        String[] parameterTypeNames = new String[]{"com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord"};
        String targetAppName = consistentInvokeRecord.getApplicationName();
        DubboUtils dubboUtils = DubboUtils.getInstance(applicationContext);
        dubboUtils.invokeRpc(targetAppName, RetryInvokeRpcService.class.getName(), "retryConsistentInvoke", parameterTypeNames, consistentInvokeRecord);
        log.info("retry consistent invoke success. id={}", consistentInvokeRecord.getId());
    }

    public Object directInvoke(Long invokeId, Object target, Method method, Object[] args) {
        try {
            Object returnObject = ReflectionUtils.invokeMethod(method, target, args);
            //访问成功，更新异步请求记录结果状态
            consistentInvokeRecordService.updateAsyncInvokeRecord(invokeId, true, null, returnObject, null);
            log.info("directInvoke {}#{} success, arguments={}, returnObject={}", target.getClass().getName(), method.getName(), args, returnObject);
            return returnObject;
        } catch (Throwable e) {
            log.error("directInvoke {}#{} error, arguments={}", target.getClass().getName(), method.getName(), args, e);
            //访问失败，更新异步请求记录结果状态，并更新下一次重试的时间
            consistentInvokeRecordService.updateAsyncInvokeRecord(invokeId, false, null, null, e.getMessage());
            throw new RuntimeException("directInvoke error.", e);
        }
    }

    /**
     * 继续执行spring aop逻辑
     * 如果执行失败才记录当前执行的meta信息，待后续job进行重试，直至成功
     * @param consistentInvokeRecord
     * @param invocation
     * @param target
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    public Object processInvoke(ConsistentInvokeRecord consistentInvokeRecord, MethodInvocation invocation, Object target, Method method, Object[] args) throws Throwable{
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            log.error("processInvoke {}#{} error, will save and retry later. arguments={}", target.getClass().getName(), method.getName(), args, e);
            consistentInvokeRecord.setStatus(ConsistentInvokeRecordStatusEnum.FAIL.getType());
            txUtils.processRequiresNew(() -> consistentInvokeRecordService.save(consistentInvokeRecord));
            throw e;
        }
    }

    public void retryInvoke(ConsistentInvokeRecord consistentInvokeRecord, boolean force) {
        if(!force) {
            if(Objects.equals(ConsistentInvokeRecordStatusEnum.SUCCESS.getType(), consistentInvokeRecord.getStatus())) {
                log.warn("this consistent invoker record have already became successful. ignored. consistentInvokeRecordId={}", consistentInvokeRecord.getId());
                return;
            }
        }

        try {
            //标识异步请求
            Class beanClass = Class.forName(consistentInvokeRecord.getClassName());
            //获取spring 容器执行对象
            Object targetBean = applicationContext.getBean(beanClass);
            String[] parameterTypeNames = JSON.parseArray(consistentInvokeRecord.getParameterTypes()).toArray(new String[]{});
            Method method = ClassUtils.getMethod(beanClass, consistentInvokeRecord.getMethodName(), parameterTypeNames);
            Object[] parameters = JSON.parseArray(consistentInvokeRecord.getArguments()).toArray();
            //转换真实类型入参
            Object[] realParameters = ClassUtils.convertRealArgs(parameters, method);
            //去掉Aop代理，否则将一直循环
            Object unwrappedTargetBean = AopProxyUtils.getSingletonTarget(targetBean);
            Object returnObject = ReflectionUtils.invokeMethod(method, unwrappedTargetBean, realParameters);
            //访问成功，更新异步请求记录结果状态
            consistentInvokeRecordService.updateAsyncInvokeRecord(consistentInvokeRecord.getId(), true, consistentInvokeRecord.getRetryCount(), returnObject, null);
            log.info("retryInvoke {}#{} success, arguments={}, returnObject={}", consistentInvokeRecord.getClassName(), consistentInvokeRecord.getMethodName(), consistentInvokeRecord.getArguments(), returnObject);
        } catch (Exception e) {
            log.error("retryInvoke {}#{} error, arguments={}", consistentInvokeRecord.getClassName(), consistentInvokeRecord.getMethodName(), consistentInvokeRecord.getArguments(), e);
            //访问失败，更新异步请求记录结果状态，并更新下一次重试的时间
            consistentInvokeRecordService.updateAsyncInvokeRecord(consistentInvokeRecord.getId(), false, consistentInvokeRecord.getRetryCount(), null, e.getMessage());
        }
    }
}
