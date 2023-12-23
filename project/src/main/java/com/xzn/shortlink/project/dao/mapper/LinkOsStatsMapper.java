package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzn.shortlink.project.dao.entity.LinkOsStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @author Nruonan
 * @description  地区数据持久层
 */
public interface LinkOsStatsMapper extends BaseMapper<LinkOsStatsDO> {
    /**
     * 记录基础访问地区监控数据
     */
    @Insert("INSERT INTO t_link_os_stats (full_short_url, gid, date, cnt ,os, create_time, update_time, del_flag) " +
        "VALUES( #{linkOsStats.fullShortUrl}, #{linkOsStats.gid}, #{linkOsStats.date}, #{linkOsStats.cnt}, #{linkOsStats.os}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE "
        + "cnt = cnt +  #{linkOsStats.cnt}; ")
    void shortLinkOsStats(@Param("linkOsStats") LinkOsStatsDO linkOsStatsDO);
}
