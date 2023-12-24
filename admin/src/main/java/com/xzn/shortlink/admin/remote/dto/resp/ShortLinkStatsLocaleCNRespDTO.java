package com.xzn.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description 短链接地区访问监控响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsLocaleCNRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;

    /**
     * 地区
     */
    private String locale;

    /**
     * 比重
     */
    private Double ratio;
}
