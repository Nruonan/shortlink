package com.xzn.shortlink.admin.remote.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * @author Nruonan
 * @description 短链接分页请求参数
 */
@Data
public class ShortLinkPageReqDTO extends Page{
    /**
     * a分页标识
     */
    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;
}
