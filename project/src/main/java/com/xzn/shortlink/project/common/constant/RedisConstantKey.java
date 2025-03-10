package com.xzn.shortlink.project.common.constant;

/**
 * @author Nruonan
 * @description
 */
public final class RedisConstantKey {
    /**
     * 短链接跳转前缀 Key
     */
    public static final String GOTO_SHORT_LINK_KEY  = "short-link_goto_%s";
    /**
     * d短链接空值跳转前端Key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_is_null_goto_%s";
    /**
     * 短链接锁前端key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s"; /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link_lock_update-gid_%s";

    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link_delay-queue:stats";

    /**
     * 短链接统计判断是否新用户缓存标识
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:";

    /**
     * 短链接统计判断是否新 IP 缓存标识
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:";

    /**
     * 创建短链接锁标识
     */
    public static final String SHORT_LINK_CREATE_LOCK_KEY = "short-link:lock:create";
}

