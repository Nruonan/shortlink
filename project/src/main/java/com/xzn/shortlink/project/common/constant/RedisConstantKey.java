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
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";
}
