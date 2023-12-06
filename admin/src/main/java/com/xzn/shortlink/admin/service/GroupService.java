package com.xzn.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import java.util.List;

/**
 * @author Nruonan
 * @description
 */
public interface GroupService extends IService<GroupDo> {

    void saveGroup(ShortLinkGroupSaveReqDTO requestParam);

    List<ShortLinkGroupRespDTO> listGroup();

    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);

    void deleteGroup(String gid);
}
