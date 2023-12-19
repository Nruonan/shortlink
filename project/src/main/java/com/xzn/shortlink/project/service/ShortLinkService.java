package com.xzn.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkCreateRespDTO;

/**
 * @author Nruonan
 * @description  短链接接口层
 */
public interface ShortLinkService extends IService<ShortLinkDO> {

    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);
}
