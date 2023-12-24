package com.xzn.shortlink.project.service;

import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

/**
 * @author Nruonan
 * @description
 */
public interface ShortLinkStatsService  {

    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);
}
