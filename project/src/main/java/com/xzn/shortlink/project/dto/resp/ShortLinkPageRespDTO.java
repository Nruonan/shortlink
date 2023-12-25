package com.xzn.shortlink.project.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * @author Nruonan
 * @description 短链接分页实体
 */
@Data
public class ShortLinkPageRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;


    /**
     * 分组标识
     */
    private String gid;
    /**
     * 网站标识
     */
    private String favicon;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private int validDateType;
    /**
     * 历史PV
     */
    private Integer totalPv;
    /**
     * 历史UV
     */
    private Integer totalUv;
    /**
     * 历史uip
     */
    private Integer totalUIp;
    /**
     * 今日pv
     */
    private Integer todayPv;

    /**
     * 今日uv
     */
    private Integer todayUv;

    /**
     * 今日ip数
     */
    private Integer todayUIp;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述
     */
    private String describe;
    /**
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
