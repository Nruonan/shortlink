package com.xzn.shortlink.admin.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.common.convention.result.Results;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.xzn.shortlink.admin.remote.dto.req.RecycleBinRecoverReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.RecycleBinRemoveReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.RecycleBinSaveReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.admin.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {
    /**
     * 后续重构为 springcloud
     */
    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    private final RecycleBinService recycleBinService;
    /**
     * 保存到回收站
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        shortRemoteLinkService.saveRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 分页回收站短链接
     */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam){
        return recycleBinService.pageRecycleBinShortLink(requestParam);
    }

    /**
     * 恢复短链接
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam){
        shortRemoteLinkService.recoverRecycleBin(requestParam);
        return Results.success();
    }
    /**
     * 恢复短链接
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/remove")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam){
        shortRemoteLinkService.removeRecycleBin(requestParam);
        return Results.success();
    }

}
