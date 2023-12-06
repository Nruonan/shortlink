package com.xzn.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;

/**
 * @author Nruonan
 * @description
 */
public interface GroupService extends IService<GroupDo> {

    void saveGroup(ShortLinkGroupSaveReqDTO requestParam);
}
