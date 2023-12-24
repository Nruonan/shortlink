package com.xzn.shortlink.project.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShortLinkStatsOsRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;
    /**
     * 操作系统类型
     */
    private String os;
    /**
     * 比重
     */
    private Double ratio;
}
