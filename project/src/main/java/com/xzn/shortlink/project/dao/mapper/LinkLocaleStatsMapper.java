package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzn.shortlink.project.dao.entity.LinkLocaleStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @author Nruonan
 * @description  地区数据持久层
 */
public interface LinkLocaleStatsMapper extends BaseMapper<LinkLocaleStatsDO> {
    /**
     * 记录基础访问地区监控数据
     */
    @Insert("INSERT INTO t_link_locale_stats (full_short_url, gid, date, cnt, province, city, adcode, country, create_time, update_time, del_flag) " +
        "VALUES( #{linkLocaleStats.fullShortUrl}, #{linkLocaleStats.gid}, #{linkLocaleStats.date}, #{linkLocaleStats.cnt}, #{linkLocaleStats.province}, #{linkLocaleStats.city}, #{linkLocaleStats.adcode}, #{linkLocaleStats.country}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE "
        + "cnt = cnt +  #{linkLocaleStats.cnt}; ")
    void shortLinkLocaleStats(@Param("linkLocaleStats") LinkLocaleStatsDO linkLocaleStatsDO);
}
