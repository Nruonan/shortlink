package com.xzn.shortlink.admin.controller;


import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description
 */
@RestController
@RequiredArgsConstructor
public class UrlTitleController {

    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    /**
     * 根据URL 获取网站标题
     */
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url){
        return shortRemoteLinkService.getTitleByUrl(url);
    }
}
