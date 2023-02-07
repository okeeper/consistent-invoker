package com.okeeper.consistentinvoker.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;


/**
 * 事务处理工具类
 * @author zhangyue
 */
@Slf4j
public class TxUtils {

    private PlatformTransactionManager platformTransactionManager;

    public TxUtils(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }

    /**
     * 使用数据库的默认隔离级别处理事务
     * @see TxUtils#process(Runnable, int)
     * @param runnable
     */
    public void processRequiresNew(Runnable runnable) {
        process(runnable, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 手动数据库事务控制
     * @param runnable 需要原子操作的可执行数据库操作
     * @param propagationBehavior 事务隔离级别, 默认是mysql的默认隔离级别级别repeatable read（可重复读）
     */
    public void process(Runnable runnable, int propagationBehavior) {
        // 事务定义
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // 设置事务的传播行为
        def.setPropagationBehavior(propagationBehavior);
        // 开启事务并获取事务状态
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            // 业务处理
            runnable.run();
            //判断是否是新开启的实物
            if(status.isNewTransaction()) {
                // 提交
                platformTransactionManager.commit(status);
            }
        } catch (Exception e) {
            //判断是否是新开启的实物
            if(status.isNewTransaction()) {
                // 回滚
                platformTransactionManager.rollback(status);
                log.error("transaction error, rollback success.", e);
            } else {
                log.error("transaction error.", e);
            }
            throw new RuntimeException("do transaction fail.");
        }
    }

}
