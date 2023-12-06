package com.xzn.shortlink.admin.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 *  短链接修改参数
 */
@Data
public class ShortLinkGroupUpdateReqDTO {
    // 分组标识
    private String gid;
    private String name;
}
