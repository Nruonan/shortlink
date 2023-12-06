package com.xzn.shortlink.admin.controller;

import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.common.convention.result.Results;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description
 */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    /**
     * 新增短链接分组
     */
    @PostMapping("/api/short-link/admin/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO requestParam){
        groupService.saveGroup(requestParam);
        return Results.success();
    }
    /**
     *  查看短链接集合
     */
    @GetMapping("/api/short-link/admin/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> listGroup(){
        List<ShortLinkGroupRespDTO> list = groupService.listGroup();
        return Results.success(list);
    }
    /**
     *  修改短链接
     */
    @PutMapping("/api/short-link/admin/v1/group")
    public Result<Void> update(@RequestBody ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO){
        groupService.updateGroup(shortLinkGroupUpdateReqDTO);
        return Results.success();
    }
    /**
     *  删除短链接
     */
    @DeleteMapping("/api/short-link/admin/v1/group")
    public Result<Void> delete(@RequestParam String gid){
        groupService.deleteGroup(gid);
        return Results.success();
    }
}
