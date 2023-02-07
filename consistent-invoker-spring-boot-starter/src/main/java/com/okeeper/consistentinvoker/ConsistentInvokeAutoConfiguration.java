package com.okeeper.consistentinvoker;

import com.okeeper.consistentinvoker.common.ConsistentConstants;
import com.okeeper.consistentinvoker.config.ConsistentInvokerConfig;
import com.okeeper.consistentinvoker.core.ConsistentInvokeAnnotationInterceptor;
import com.okeeper.consistentinvoker.core.ConsistentInvokeHandler;
import com.okeeper.consistentinvoker.core.dao.ConsistentInvokeRecordMapper;
import com.okeeper.consistentinvoker.core.dao.impl.ConsistentInvokeRecordMapperImpl;
import com.okeeper.consistentinvoker.core.service.ConsistentInvokeRecordService;
import com.okeeper.consistentinvoker.job.AutoRetryConsistentInvokeJobHandler;
import com.okeeper.consistentinvoker.rpc.RetryInvokeRpcService;
import com.okeeper.consistentinvoker.rpc.impl.RetryInvokeRpcServiceImpl;
import com.okeeper.consistentinvoker.utils.DubboUtils;
import com.alibaba.druid.pool.DruidDataSource;
import com.xxl.job.core.handler.IJobHandler;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author yue
 */
@EnableDubbo
@ConditionalOnProperty(prefix = "consistent-invoker", name = "enable", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(ConsistentInvokerConfig.class)
public class ConsistentInvokeAutoConfiguration {

    @Autowired
    private ConsistentInvokerConfig consistentInvokerConfig;

    private DataSource consistentDatasource;

    /**
     * 获取持久化的数据源
     * @param applicationContext
     * @return
     */
    private DataSource getConsistentDatasource(ApplicationContext applicationContext) {
        //是否使用默认数据源
        if (consistentInvokerConfig.isUseDefaultDatasource()) {
            return applicationContext.getBean(DataSource.class);
        } else {
            if(consistentDatasource == null) {
                DruidDataSource dataSource = consistentInvokerConfig.getDatasource();
                if(Objects.isNull(dataSource) || Objects.isNull(dataSource.getUrl()) || Objects.isNull(dataSource.getUsername())) {
                    throw new IllegalArgumentException("useDefaultDatasource is false, pls config the consistent-invoker.datasource for init database.");
                }
                //初始化一致性框架自己的数据源
                try {
                    dataSource.init();
                    consistentDatasource = dataSource;
                } catch (SQLException e) {
                    throw new IllegalArgumentException("init datasource error.", e);
                }
            }
            return consistentDatasource;
        }
    }


    /**
     * 初始化dao
     * @return
     * @throws Exception
     */
    @Bean
    public ConsistentInvokeRecordMapper consistentInvokeRecordMapper(ApplicationContext applicationContext) throws Exception {
        return new ConsistentInvokeRecordMapperImpl(getConsistentDatasource(applicationContext),
                applicationContext.getResource("classpath:com/okeeper/consistentinvoker/core/dao/ConsistentInvokeRecordMapper.xml")
        );
    }

    /**
     * 初始化Service
     * @return
     * @throws Exception
     */
    @ConditionalOnMissingBean
    @Bean
    public ConsistentInvokeRecordService consistentInvokeRecordService(ConsistentInvokeRecordMapper consistentInvokeRecordMapper, ConsistentInvokerConfig consistentInvokerConfig) {
        return new ConsistentInvokeRecordService(consistentInvokeRecordMapper, consistentInvokerConfig);
    }


    /**
     * handler
     * @return
     * @throws Exception
     */
    @ConditionalOnMissingBean
    @Bean
    public ConsistentInvokeHandler consistentInvokeHandler(){
        return new ConsistentInvokeHandler();
    }


    /**
     * 是否开启重试任务
     * @return
     */
    @ConditionalOnClass(IJobHandler.class)
    @ConditionalOnProperty(prefix = "consistent-invoker", name = "enableJob", havingValue = "true")
    @Bean
    public AutoRetryConsistentInvokeJobHandler autoRetryConsistentInvokeJobHandler() {
        return new AutoRetryConsistentInvokeJobHandler();
    }

    /**
     * @ConsistentInvoke AOP注解拦截实现
     * 最终一致性请求注解实现拦截器配置
     * @return
     */
    @Bean
    public DefaultPointcutAdvisor consistentInvokeAnnotationAdvisor(ConsistentInvokeHandler consistentInvokeHandler) {
        DefaultPointcutAdvisor advisor =  new DefaultPointcutAdvisor();
        AspectJExpressionPointcut aspectJExpressionPointcut = new AspectJExpressionPointcut();
        aspectJExpressionPointcut.setExpression("@annotation(com.okeeper.consistentinvoker.core.ConsistentInvoke)");
        advisor.setPointcut(aspectJExpressionPointcut);
        advisor.setAdvice(new ConsistentInvokeAnnotationInterceptor(consistentInvokeHandler));
        return advisor;
    }


    /**
     * @ConsistentInvoke AOP注解拦截实现
     * 最终一致性请求注解实现拦截器配置
     * @return
     */
    @ConditionalOnProperty(value = "consistent-invoker.enableRetryProvider", havingValue = "true", matchIfMissing = true)
    @Bean
    public RetryInvokeRpcService retryInvokeRpcService(ConsistentInvokeHandler consistentInvokeHandler, ApplicationContext applicationContext) {
        RetryInvokeRpcService retryInvokeRpcService = new RetryInvokeRpcServiceImpl(consistentInvokeHandler);
        DubboUtils.getInstance(applicationContext).exportInterface(RetryInvokeRpcService.class, retryInvokeRpcService);
        return retryInvokeRpcService;
    }


    /**
     * 线程池配置
     * @return
     */
    @Bean(ConsistentConstants.CONSISTENT_INVOKE_THREAD_POOL)
    public ThreadPoolTaskExecutor asyncInvokeTheadPool() {

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        //核心线程数
        taskExecutor.setCorePoolSize(5);
        //线程池维护线程的最大数量,只有在缓冲队列满了之后才会申请超过核心线程数的线程
        taskExecutor.setMaxPoolSize(20);
        //缓存队列
        taskExecutor.setQueueCapacity(100);
        //允许的空闲时间,当超过了核心线程数之外的线程在空闲时间到达之后会被销毁
        taskExecutor.setKeepAliveSeconds(200);
        //异步方法内部线程名称
        taskExecutor.setThreadNamePrefix("async-invoke-");

        /**
         * 当线程池的任务缓存队列已满并且线程池中的线程数目达到maximumPoolSize，如果还有任务到来就会采取任务拒绝策略
         * 通常有以下四种策略：
         * ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。
         * ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。
         * ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
         * ThreadPoolExecutor.CallerRunsPolicy：重试添加当前的任务，自动重复调用 execute() 方法，直到成功
         */
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        taskExecutor.initialize();

        return taskExecutor;
    }
}
