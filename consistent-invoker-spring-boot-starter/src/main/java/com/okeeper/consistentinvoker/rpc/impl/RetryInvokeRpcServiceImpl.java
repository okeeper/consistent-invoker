package com.okeeper.consistentinvoker.rpc.impl;

import com.okeeper.consistentinvoker.core.ConsistentInvokeHandler;
import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;
import com.okeeper.consistentinvoker.rpc.RetryInvokeRpcService;

/**
 * 重试
 * @author yue
 */
public class RetryInvokeRpcServiceImpl implements RetryInvokeRpcService {

    private ConsistentInvokeHandler consistentInvokeHandler;

    public RetryInvokeRpcServiceImpl(ConsistentInvokeHandler consistentInvokeHandler) {
        this.consistentInvokeHandler = consistentInvokeHandler;
    }

    @Override
    public void retryConsistentInvoke(ConsistentInvokeRecord consistentInvokeRecord) {
        consistentInvokeHandler.retryInvoke(consistentInvokeRecord, consistentInvokeRecord.isForce());
    }
}
