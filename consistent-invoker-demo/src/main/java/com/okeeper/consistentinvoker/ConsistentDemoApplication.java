package com.okeeper.consistentinvoker;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.mapper.autoconfigure.MapperAutoConfiguration;

@EnableDubbo
@SpringBootApplication(exclude = {MapperAutoConfiguration.class})
public class ConsistentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsistentDemoApplication.class, args);
    }

}
