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
public class ShortLinkStatsDeviceRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;
    /**
     * 设备类型
     */
    private String device;
    /**
     * 比重
     */
    private Double ratio;
}
