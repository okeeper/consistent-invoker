package com.okeeper.consistentinvoker.core.service;

import com.okeeper.consistentinvoker.core.ConsistentInvokeRecordStatusEnum;
import com.okeeper.consistentinvoker.config.ConsistentInvokerConfig;
import com.okeeper.consistentinvoker.utils.TraceIdUtil;
import com.okeeper.consistentinvoker.core.dao.ConsistentInvokeRecordMapper;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.okeeper.consistentinvoker.utils.FibonacciUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 异步接口请求记录service
 * @author yue
 */
public class ConsistentInvokeRecordService {

    @Autowired
    private ConsistentInvokeRecordMapper consistentInvokeRecordMapper;

    @Autowired
    private ConsistentInvokerConfig consistentInvokerConfig;

    public ConsistentInvokeRecordService(ConsistentInvokeRecordMapper consistentInvokeRecordMapper, ConsistentInvokerConfig consistentInvokerConfig) {
        this.consistentInvokeRecordMapper = consistentInvokeRecordMapper;
        this.consistentInvokerConfig = consistentInvokerConfig;
    }

    public void save(ConsistentInvokeRecord consistentInvokeRecord) {
        consistentInvokeRecord.setCreateTime(new Date());
        consistentInvokeRecord.setUpdateTime(new Date());
        consistentInvokeRecord.setRetryCount(0);
        consistentInvokeRecord.setNextRetryTime(new Date());
        consistentInvokeRecord.setStatus(ConsistentInvokeRecordStatusEnum.WAIT_INVOKE.getType());
        consistentInvokeRecordMapper.insertSelective(consistentInvokeRecord);
    }

    public ConsistentInvokeRecord save(Class clazz, Method method, Object[] arguments, int maxRetryCount, String methodParameterValue) {
        ConsistentInvokeRecord consistentInvokeRecord = buildAsyncInvokeRecord(null, clazz, method, arguments, maxRetryCount, methodParameterValue);
        this.save(consistentInvokeRecord);
        return consistentInvokeRecord;
    }

    /**
     * 获取rpc请求接口的定义
     * @param applicationName
     * @param clazz
     * @param method
     * @param args
     * @return
     */
    public ConsistentInvokeRecord buildAsyncInvokeRecord(String applicationName, Class clazz, Method method, Object[] args, int maxRetryCount, String keys) {
        java.lang.String[] parameterTypes = Stream.of(method.getParameterTypes()).map(Class::getName).toArray(String[]::new);
        return ConsistentInvokeRecord.builder()
                .tid(TraceIdUtil.getTraceId())
                .applicationName(applicationName)
                .className(clazz.getName())
                .methodName(method.getName())
                .parameterTypes(JSON.toJSONString(parameterTypes))
                .arguments(JSON.toJSONString(args))
                .invokeKey(keys != null ? keys : TraceIdUtil.getTraceId())
                .maxRetryCount(maxRetryCount != -1 ? maxRetryCount : consistentInvokerConfig.getInvokeRetryCount())
                .build();
    }


    /**
     * 通过id查询异步请求记录
     * @param id
     * @return
     */
    public ConsistentInvokeRecord queryById(Long id) {
        return consistentInvokeRecordMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新记录状态
     * @param id rpc请求记录id
     * @param invokeSuccess 是否成功
     * @param hasRetryCount 已经重试的次数
     * @param returnObject 结果
     */
    public void updateAsyncInvokeRecord(Long id, boolean invokeSuccess, Integer hasRetryCount, Object returnObject, String message) {
        ConsistentInvokeRecord consistentInvokeRecord = ConsistentInvokeRecord.builder()
                .id(id)
                .tid(TraceIdUtil.getTraceId())
                .retryCount(hasRetryCount)
                .returnObject(returnObject != null ? JSON.toJSONString(returnObject) : null)
                .errorMessage(Objects.nonNull(message)  && message.length() > 500? message.substring(0, 500) : message)
                .maxRetryCount(consistentInvokerConfig.getInvokeRetryCount())
                .status(invokeSuccess ? ConsistentInvokeRecordStatusEnum.SUCCESS.getType() : ConsistentInvokeRecordStatusEnum.FAIL.getType())
                .build();
        //计算下一次重试请求时间
        if(!invokeSuccess) {
            //按斐波拉契数列进阶，进阶单位为1分钟
            if(hasRetryCount != null) {
                consistentInvokeRecord.setNextRetryTime(DateUtils.addMinutes(new Date(), Math.min(FibonacciUtils.fib(hasRetryCount) * consistentInvokerConfig.getInvokeRetryStepScaleMinute(), consistentInvokerConfig.getInvokeRetryIntervalMaxMinute())));
            } else {
                consistentInvokeRecord.setNextRetryTime(DateUtils.addMinutes(new Date(), consistentInvokerConfig.getInvokeRetryStepScaleMinute()));
            }
        }
        consistentInvokeRecordMapper.updateByPrimaryKeySelective(consistentInvokeRecord);
    }

    /**
     * 查询下一次要重试的请求记录
     * @param statusList
     * @return
     */
    public List<ConsistentInvokeRecord> queryWaitInvokeListByNextRetryTimePageList(List<Integer> statusList, Integer pageSize) {
        return consistentInvokeRecordMapper.queryWaitInvokeListByNextRetryTimePageList(statusList, 0, pageSize);
    }

    /**
     * 物理删除
     * @param id
     */
    public void delete(Long id) {
        consistentInvokeRecordMapper.delete(id);
    }

    /**
     * 注册事物管理器
     * @param transactionSynchronizationAdapter
     */
    @Transactional(rollbackFor = Exception.class)
    public void registerInvokeRecordWitBizTransaction(TransactionSynchronizationAdapter transactionSynchronizationAdapter) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronizationAdapter);
        }else {
            throw new RuntimeException("no transaction manager found");
        }
    }
}
