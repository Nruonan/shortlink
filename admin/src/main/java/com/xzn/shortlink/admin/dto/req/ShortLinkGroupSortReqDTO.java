package com.xzn.shortlink.admin.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 *  短链接返回实体
 */
@Data
public class ShortLinkGroupSortReqDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组排序
     */
    private Integer sortOrder;

}
