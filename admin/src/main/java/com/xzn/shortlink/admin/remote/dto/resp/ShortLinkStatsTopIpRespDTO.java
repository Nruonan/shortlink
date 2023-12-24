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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkStatsTopIpRespDTO {
    /**
     * 访问次数
     */
    private Integer cnt;

    /**
     * ip地址
     */
    private String ip;
}
