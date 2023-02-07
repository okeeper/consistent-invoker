package com.okeeper.consistentinvoker.rpc;

import com.okeeper.consistentinvoker.core.model.ConsistentInvokeRecord;

/**
 * @author yue
 */
public interface RetryInvokeRpcService {

    /**
     * 重试单条记录
     * @param consistentInvokeRecord
     * @return
     */
    void retryConsistentInvoke(ConsistentInvokeRecord consistentInvokeRecord);
}
