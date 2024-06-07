package com.xzn.shortlink.gateway.config;

import java.util.List;
import lombok.Data;

/**
 * @author Nruonan
 * @description 过滤器配置
 */
@Data
public class Config {

    /**
     * 白名单前置路径
     */
    private List<String> whitePathList;
}
