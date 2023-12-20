package com.xzn.shortlink.admin;

/**
 * @author Nruonan
 * @description
 */
public class Test {
    static String sql = "CREATE TABLE `t_link_goto_%d`  (\n"
        + "  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',\n"
        + "  `gid` varchar(32) DEFAULT 'default' COMMENT '分组标识',\n"
        + " `full_short_url` varchar(128) DEFAULT NULL COMMENT '完整短链接',\n"
        + " PRIMARY KEY(`id`)\n"
        + ") ENGINE = InnoDB  DEFAULT CHARSET = utf8mb4 ;;";

    public static void main(String[] args) {
        for(int i =0; i < 16; i++){
            System.out.printf(sql,i);
        }
    }
}
