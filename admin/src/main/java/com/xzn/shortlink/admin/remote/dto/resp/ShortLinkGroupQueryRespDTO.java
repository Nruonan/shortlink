package com.xzn.shortlink.admin.remote.dto.resp;

import lombok.Data;

/**
 * @author Nruonan
 * @description
 */
@Data
public class ShortLinkGroupQueryRespDTO {
    private String gid;
    private Integer shortLinkCount;
}
