package com.xzn.shortlink.project.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 * @description 回收站恢复功能
 */
@Data
public class RecycleBinRecoverReqDTO {
    /**
     * 分组标识
     */
    private String gid;


    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
