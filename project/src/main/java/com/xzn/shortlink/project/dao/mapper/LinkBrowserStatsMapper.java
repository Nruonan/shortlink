package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzn.shortlink.project.dao.entity.LinkBrowserStatsDO;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Nruonan
 * @description  地区数据持久层
 */
public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {
    /**
     * 记录基础访问地区监控数据
     */
    @Insert("INSERT INTO t_link_browser_stats (full_short_url, gid, date, cnt ,browser, create_time, update_time, del_flag) " +
        "VALUES( #{linkBrowserStats.fullShortUrl}, #{linkBrowserStats.gid}, #{linkBrowserStats.date}, #{linkBrowserStats.cnt}, #{linkBrowserStats.browser}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE "
        + "cnt = cnt +  #{linkBrowserStats.cnt}; ")
    void shortLinkBrowserStats(@Param("linkBrowserStats") LinkBrowserStatsDO linkBrowserStatsDO);

    /**
     * 根据短链接获取指定日期内浏览器监控数据
     */
    @Select("SELECT " +
        "    browser, " +
        "    SUM(cnt) AS count " +
        "FROM " +
        "    t_link_browser_stats " +
        "WHERE " +
        "    full_short_url = #{param.fullShortUrl} " +
        "    AND gid = #{param.gid} " +
        "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
        "GROUP BY " +
            "    full_short_url, gid, browser;")
    List<HashMap<String, Object>> listBrowserStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据小组获取指定日期内浏览器监控数据
     */
    @Select("SELECT " +
        "    browser, " +
        "    SUM(cnt) AS count " +
        "FROM " +
        "    t_link_browser_stats " +
        "WHERE " +
        "     gid = #{param.gid} " +
        "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
        "GROUP BY " +
        "     gid, browser;")
    List<HashMap<String, Object>> listBrowserStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);
}
