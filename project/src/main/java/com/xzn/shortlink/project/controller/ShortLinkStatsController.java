package com.xzn.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.project.common.convention.result.Result;
import com.xzn.shortlink.project.common.convention.result.Results;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import com.xzn.shortlink.project.service.ShortLinkStatsService;
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

    private final ShortLinkStatsService shortLinkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam){
        return Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
    }
    /**
     * 访问组内短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam){
        return Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
    }
    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam){
        return Results.success(shortLinkStatsService.oneShortLinkStatsAccessRecord(requestParam));
    }

    /**
     * 访问分组指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkGroupStatsAccessRecord(
        ShortLinkGroupStatsAccessRecordReqDTO requestParam){
        return Results.success(shortLinkStatsService.groupShortLinkStatsAccessRecord(requestParam));
    }
}
