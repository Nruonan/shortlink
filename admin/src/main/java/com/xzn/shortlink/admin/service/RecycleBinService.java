package com.xzn.shortlink.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description
 */
@Service
public interface RecycleBinService {

    Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
