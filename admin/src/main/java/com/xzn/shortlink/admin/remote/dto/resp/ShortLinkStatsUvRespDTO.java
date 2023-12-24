package com.xzn.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description 新旧用户
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShortLinkStatsUvRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;
    /**
     * 用户类型
     */
    private String uvType;
    /**
     * 比重
     */
    private Double ratio;
}
