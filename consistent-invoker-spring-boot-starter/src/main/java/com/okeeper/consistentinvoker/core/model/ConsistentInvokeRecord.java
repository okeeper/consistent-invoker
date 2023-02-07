package com.okeeper.consistentinvoker.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 远程方法调用的接口描述及元数据信息
 * @author zhangyue
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ConsistentInvokeRecord implements Serializable {

    /**
     * 异步方法访问的记录id
     */
    private Long id;

    /**
     * 请求的关键字，方便查找搜索
     */
    private String invokeKey;

    /**
     * tid
     */
    private String tid;

    /**
     * 应用名
     */
    private String applicationName;

    /**
     * 接口名称
     */
    private String className;
    /**
     * 方法
     */
    private String methodName;
    /**
     * 接口定义的参数类型
     */
    private String parameterTypes;

    /**
     * 请求参数
     */
    private String arguments;

    /**
     * 请求是否成功 0 待请求 1失败 2成功
     */
    private Integer status;

    /**
     * 最后一次响应内容
     */
    private String returnObject;

    /**
     * 访问的错误消息
     */
    private String errorMessage;

    /**
     * springEl表达式
     * 例如：(#alResponse.resultIsSuccess() and #alResponse.data?.status == 100) or #alResponse.code == '11211810000244'
     * 表示当alResponse返回值请求成功或者异常码等于 11211810000244 认为方位成功
     */
    private String springEl;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大请求次数
     */
    private Integer maxRetryCount;

    /**
     * 下一次重试时间
     */
    private Date nextRetryTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否强制访问
     */
    private boolean force;
}
