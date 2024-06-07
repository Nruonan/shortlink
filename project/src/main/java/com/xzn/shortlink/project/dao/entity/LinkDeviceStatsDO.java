package com.xzn.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xzn.shortlink.project.common.database.BaseDO;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description 访问设备
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_link_device_stats")
public class LinkDeviceStatsDO extends BaseDO {
    /**
     * id
     */
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;



    /**
     * 日期
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer cnt;

    /**
     * 访问设备
     */
    private String device;


}
