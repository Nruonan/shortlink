package com.xzn.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkStatsReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {
    /**
     * 后续重构为 springcloud
     */
    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam){
        return shortRemoteLinkService.oneShortLinkStats(requestParam);
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(
        ShortLinkStatsAccessRecordReqDTO requestParam){
        return shortRemoteLinkService.oneShortLinkStatsAccessRecord(requestParam);
    }
}
