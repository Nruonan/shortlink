package com.xzn.shortlink.admin.remote.dto.resp;

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
public class ShortLinkStatsBrowserRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;
    /**
     * 浏览器类型
     */
    private String browser;
    /**
     * 比重
     */
    private Double ratio;
}
