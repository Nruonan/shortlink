package com.xzn.shortlink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Nruonan
 * @description
 */
@SpringBootApplication
@MapperScan("com.xzn.shortlink.admin.dao.mapper")
public class ShortLinkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkAdminApplication.class,args);
    }
}
