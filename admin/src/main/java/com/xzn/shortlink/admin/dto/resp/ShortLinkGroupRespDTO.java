package com.xzn.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * @author Nruonan
 *  短链接返回实体
 */
@Data
public class ShortLinkGroupRespDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;



    /**
     * 分组排序
     */
    private Integer sortOrder;

}
