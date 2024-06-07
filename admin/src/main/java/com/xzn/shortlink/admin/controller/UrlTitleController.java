package com.xzn.shortlink.admin.controller;


import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.remote.dto.ShortLinkActualRemoteService;
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

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;
    /**
     * 根据URL 获取网站标题
     */
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url){
        return shortLinkActualRemoteService.getTitleByUrl(url);
    }
}
