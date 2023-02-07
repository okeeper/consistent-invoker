# What is `consistent-invoker-spring-boot-starter`
这是一个支持可持久化，对业务库无入侵的纯中间件式的可重试框架
- 支持任何spring管理的bean方法。
- 使用注解方式方便快速使用
- 支持指定重试次数，
- 支持使用SpringEL表达式指定重试记录的关键字
- 零代码开发量，即引用即使用，即配置即生效

# Quick start

## 一、 引入maven依赖

```xml
 <dependency>
      <groupId>com.okeeper</groupId>
      <artifactId>consistent-invoker-spring-boot-starter</artifactId>
      <version>1.0-SNAPSHOT</version>
 </dependency>
```

## 二、 在spring上下文中配置

最终一致性可重试框架支持两种模式：
- 一种是使用专门给重试用的独立数据库，独立维护
- 一种是和业务数据库共用数据库


### 1.当使用独立数据库
#### 1.1 配置`use-default-datasource=false`,并配置独立数据库数据源
```yaml
consistent-invoker:
  enable: true
  # 是否启动xxl job,为true时将初始化XXlJob实现类到spring 容器中
  enableJob: false
  # 是否注册重试服务提供者到dubbo中，为true时需配置dubbo、注册中心等配置
  enableRetryProvider: false
  # 使用业务数据库，即spring的默认数据源
  use-default-datasource: false

  # 选配，当use-default-datasource=false时必须配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/consistent_call_db?characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true
    username: root
    password: xxxx
```

#### 1.2 ddl初始化：

```sql
-- 创建数据库
create database 'consistent_invoker_db';
```

```sql
-- 新增最终一致访问请求记录表
CREATE TABLE `t_consistent_invoker_record` (
  `id` bigint(11) NOT NULL auto_increment COMMENT '远程方法访问的记录id',
  `invoke_key` varchar(255) DEFAULT NULL COMMENT '请求的关键字，方便查找搜索',
  `tid` varchar(255) DEFAULT NULL COMMENT 'tid',
  `application_name` varchar(255) DEFAULT NULL COMMENT '重试应用名，对应dubbo注册服务名',
  `class_name` varchar(255) DEFAULT NULL COMMENT '接口名称',
  `method_name` varchar(255) DEFAULT NULL COMMENT '方法',
  `parameter_types` varchar(512) DEFAULT NULL COMMENT '接口定义的参数类型',
  `arguments` text DEFAULT NULL COMMENT '请求参数',
  `status` TINYINT DEFAULT NULL COMMENT '请求是否成功 0 待请求 1失败 2成功',
  `spring_el` varchar(255) DEFAULT NULL COMMENT 'spring EL 表达式判断',
  `return_object` text COMMENT '最后一次响应内容',
  `error_message` varchar(512) default null comment '错误信息',
  `retry_count` int(11) DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime(2) DEFAULT NULL COMMENT '下一次重试时间',
  `max_retry_count` int(11) DEFAULT '16' COMMENT '最大请求次数',
  `create_time` datetime(2) DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(2) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_consitent_invoke_record_invoke_key`(`invoke_key`) USING HASH COMMENT '关键字索引',
  KEY `idx_consitent_invoke_record_next_retry_time`(`next_retry_time`) COMMENT '下一次重试时间索引',
  KEY `idx_consitent_invoke_record_create_time`(`create_time`) COMMENT '创建时间索引',
  KEY `idx_consitent_invoke_record_method`(`class_name`,`method_name`) COMMENT '方法索引',
  KEY `idx_consitent_invoke_record_tid`(`tid`) COMMENT 'tid索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步请求记重试表';
```

#### 1.3 配置定时任务补偿
如果使用的共用独立的数据库持久化，此步骤可忽略，因为已经配置了，如果不是，开启定时任务配置

```yaml
consistent-invoker:
  enable: true
  enableJob: true
```

#### 1.4 xxl-job console新建执行器和任务
- 新建执行器：
```
appName: consistent-job-application
名称： 最终一致性重试job应用
注册方式：自动注册


- 新建任务：
执行器：最终一致性重试job应用
任务描述：重试失败的方法访问
路由策略：轮询
Cron: 0 0/5 * * * ?
JobHandler: autoRetryConsistentInvokeJobHandler

```

### 2. 与业务数据库共用数据库时
#### 2.1 配置`use-default-datasource=true`即可
```yaml
consistent-invoker:
  enable: true
  enableJob: false
  # 使用业务数据库，即spring的默认数据源
  use-default-datasource: true
```

## 三、在项目中使用`@ConsistentInvoke`注解

```java
    @ConsistentInvoke(key = "#sku")
    public Object offlineSkuNonTxAndSyncMode(Long sku){
        log.info("offlineSku:{}", sku);
        if(sku == null || sku < 0){
            throw new RuntimeException("call failed");
        }
        return "offlineSku success";
    }
```

## 四、重试
当系统自动重试次数（默认16次）过后还是失败时，将不在重试。
如果此时需要手动进行重试时，在xxl-job工作台传入对应的invokeId即可，如果要强制重试（不关心成功与否）用`force,$invokeId`进行


# Demo启动

1. 修改`consistent-invoker-demo`模块中的application.yml的数据库配置
```agsl

```