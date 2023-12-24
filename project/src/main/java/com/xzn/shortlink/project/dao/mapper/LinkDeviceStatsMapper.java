package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzn.shortlink.project.dao.entity.LinkDeviceStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

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
}
