package com.xzn.shortlink.project.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 * @description  移除短链接
 */
@Data
public class RecycleBinRemoveReqDTO {
    /**
     * 分组标识
     */
    private String gid;


    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
