package com.xzn.shortlink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Nruonan
 * @description
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients("com.xzn.shortlink.admin.remote")
@MapperScan("com.xzn.shortlink.admin.dao.mapper")
public class ShortLinkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkAdminApplication.class,args);
    }
}
