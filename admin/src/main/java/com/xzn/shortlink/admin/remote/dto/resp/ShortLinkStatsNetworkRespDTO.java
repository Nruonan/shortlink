package com.xzn.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsNetworkRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;

    /**
     * 网络类型
     */
    private String network;

    /**
     * 比重
     */
    private Double ratio;
}
