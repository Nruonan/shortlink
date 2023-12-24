package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzn.shortlink.project.dao.entity.LinkDeviceStatsDO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Nruonan
 * @description  访问设备监控持久层
 */
public interface LinkDeviceStatsMapper extends BaseMapper<LinkDeviceStatsDO> {
    /**
     * 记录基础访问地区监控数据
     */
    @Insert("INSERT INTO t_link_device_stats (full_short_url, gid, date, cnt ,device, create_time, update_time, del_flag) " +
        "VALUES( #{linkDeviceStats.fullShortUrl}, #{linkDeviceStats.gid}, #{linkDeviceStats.date}, #{linkDeviceStats.cnt}, #{linkDeviceStats.device}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE "
        + "cnt = cnt +  #{linkDeviceStats.cnt}; ")
    void shortLinkDeviceStats(@Param("linkDeviceStats") LinkDeviceStatsDO linkDeviceStatsDO);

    /**
     * 根据短链接获取指定日期内设备监控数据
     */
    @Select("SELECT " +
        "    device, " +
        "    SUM(cnt) AS count " +
        "FROM " +
        "    t_link_device_stats " +
        "WHERE " +
        "    full_short_url = #{param.fullShortUrl} " +
        "    AND gid = #{param.gid} " +
        "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
        "GROUP BY " +
        "    full_short_url, device, gid;")
    List<HashMap<String, Object>>  listDeviceStatsByShortLink(@Param("param") ShortLinkStatsReqDTO linkReqDTO);
}
