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
import com.okeeper.consistentinvoker.utils.TxUtils;
import com.xxl.job.core.handler.IJobHandler;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.aspectj.lang.annotation.Aspect;
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
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author yue
 */
@Aspect
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
    //最低优先级执行
    @Order
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

    @Bean
    public TxUtils txUtils(ApplicationContext applicationContext) {
        PlatformTransactionManager platformTransactionManager = new DataSourceTransactionManager(getConsistentDatasource(applicationContext));
        return new TxUtils(platformTransactionManager);
    }
}
