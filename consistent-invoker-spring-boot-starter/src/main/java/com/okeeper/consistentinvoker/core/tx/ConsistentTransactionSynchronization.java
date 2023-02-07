package com.okeeper.consistentinvoker.core.tx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.concurrent.Callable;

/**
 * 分布式使用同步协调
 *
 * @author zhangyue
 */
@Slf4j
public class ConsistentTransactionSynchronization extends TransactionSynchronizationAdapter {

    private Callable commitCallback;
    private Callable rollbackCallback;

    public ConsistentTransactionSynchronization(Callable commitCallback, Callable rollbackCallback) {
        this.commitCallback = commitCallback;
        this.rollbackCallback = rollbackCallback;
    }

    @Override
    public void afterCommit() {
        //执行异步回调
        try {
            commitCallback.call();
        } catch (Exception e) {
            log.error("commitCallback error", e);
        }
    }

    @Override
    public void afterCompletion(int status) {
        if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
            try {
                rollbackCallback.call();
            } catch (Exception e) {
                log.error("rollbackCallback error", e);
            }
        }
    }
}
