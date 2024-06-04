package com.xzn.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.dto.req.ShortLinkBatchCreateReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkBaseInfoRespDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkBatchCreateRespDTO;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.admin.toolkit.EasyExcelWebUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description 短链接后管控制层
 */
@RestController
public class ShortLinkController {
    /**
     * 后续重构为 springcloud
     */
    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    /**
     *  创建短链接
     */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return shortRemoteLinkService.createShortLink(requestParam);
    }
    /**
     * 批量创建短链接
     */
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response){
        Result<ShortLinkBatchCreateRespDTO> result = shortRemoteLinkService.batchCreateShortLink(requestParam);
        if (result.isSuccess()){
            List<ShortLinkBaseInfoRespDTO> list = result.getData().getBaseLinkInfos();
            EasyExcelWebUtil.write(response,"批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, list);
        }
    }
    /**
     *  修改短链接
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> createShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        return shortRemoteLinkService.updateShortLink(requestParam);
    }
    /**
     * 分页查询短链接
     */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        return shortRemoteLinkService.pageShortLink(requestParam);
    }

}
