package com.xzn.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

/**
 * @author Nruonan
 * @description
 */
public interface ShortLinkStatsService  {

    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    IPage<ShortLinkStatsAccessRecordRespDTO> oneShortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);

    ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam);
}
