package com.xzn.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * @author Nruonan
 * @description 回收站管理接口层
 */
public interface RecycleBinService extends IService<ShortLinkDO> {


    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
