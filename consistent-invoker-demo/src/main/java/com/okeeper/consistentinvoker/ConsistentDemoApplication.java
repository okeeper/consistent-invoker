package com.okeeper.consistentinvoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.mapper.autoconfigure.MapperAutoConfiguration;

@SpringBootApplication(exclude = {MapperAutoConfiguration.class})
public class ConsistentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsistentDemoApplication.class, args);
    }

}
