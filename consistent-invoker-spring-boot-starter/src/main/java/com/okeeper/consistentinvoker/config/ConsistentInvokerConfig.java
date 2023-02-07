package com.okeeper.consistentinvoker.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 配置
 * @author yue
 */
@Configuration
@ConfigurationProperties(prefix = "consistent-invoker")
@Data
public class ConsistentInvokerConfig {

    /**
     * 默认false
     */
    private boolean useDefaultDatasource = false;

    /**
     * 默认rpc请求重试次数
     */
    private Integer invokeRetryCount = 16;

    /**
     * 重试步长刻度，单位分钟
     */
    private Integer invokeRetryStepScaleMinute = 2;

    /**
     * 重试最大间隔时间，单位分钟
     */
    private Integer invokeRetryIntervalMaxMinute = 1440;

    /**
     * 重试批次数量
     */
    private Integer retryBatchSize = 100;

    /**
     * 数据源类型
     */
    private String datasourceType = "druid";

    /**
     * 数据源配置
     */
    private DruidDataSource datasource = new DruidDataSource();

}

