package com.okeeper.consistentinvoker.demo.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.autoconfigure.MybatisProperties;
import tk.mybatis.mapper.autoconfigure.SpringBootVFS;

import javax.sql.DataSource;

/**
 * @author zhangyue
 */
@Configuration
public class MybatisConfig {

    @Configuration
    @tk.mybatis.spring.annotation.MapperScan(basePackages = "com.okeeper.consistentinvoker.demo.dao", sqlSessionFactoryRef = "ds1SqlSessionFactory")
    public static class DataSource1MybatisConfig {

        @Bean("mybatisProperties")
        @ConfigurationProperties(prefix = "mybatis")
        public MybatisProperties ds1MybatisProperties(){
            return new MybatisProperties();
        }

        @Bean
        public SqlSessionFactory ds1SqlSessionFactory(DataSource dataSource, ResourceLoader resourceLoader, @Qualifier("mybatisProperties") MybatisProperties properties) throws Exception {
            return newSqlSessionFactory(dataSource, resourceLoader, properties);
        }
    }

    public static SqlSessionFactory newSqlSessionFactory(DataSource dataSource, ResourceLoader resourceLoader, MybatisProperties properties) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setVfs(SpringBootVFS.class);

        if (StringUtils.hasText(properties.getConfigLocation())) {
            sessionFactory.setConfigLocation(resourceLoader.getResource(properties.getConfigLocation()));
        }

        if (properties.getConfigurationProperties() != null) {
            sessionFactory.setConfigurationProperties(properties.getConfigurationProperties());
        }
        return sessionFactory.getObject();
    }
}
