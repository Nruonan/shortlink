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
 * @description 地区数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_link_os_stats")
public class LinkOsStatsDO extends BaseDO {
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
     * 操作系统
     */
    private String os;


}
