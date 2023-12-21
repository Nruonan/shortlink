package com.xzn.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dto.req.RecycleBinSaveReqDTO;

/**
 * @author Nruonan
 * @description 回收站管理接口层
 */
public interface RecycleBinService extends IService<ShortLinkDO> {


    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}
