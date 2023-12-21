package com.xzn.shortlink.project.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 * @description
 */
@Data
public class RecycleBinSaveReqDTO {
    /**
     * 分组标识
     */
    private String gid;


    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
