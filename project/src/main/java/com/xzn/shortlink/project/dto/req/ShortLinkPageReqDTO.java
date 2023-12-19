package com.xzn.shortlink.project.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

/**
 * @author Nruonan
 * @description 短链接分页请求参数
 */
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {
    /**
     * a分页标识
     */
    private String gid;
}
