package com.xzn.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xzn.shortlink.admin.common.biz.user.UserContext;
import com.xzn.shortlink.admin.common.convention.exception.ServiceException;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.admin.service.RecycleBinService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {
    private final GroupMapper groupMapper;
    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getUsername, UserContext.getUsername())
            .eq(GroupDo::getDelFlag, 0);
        List<GroupDo> groupDoList = groupMapper.selectList(queryWrapper);
        if(CollUtil.isEmpty(groupDoList)){
            throw new ServiceException("用户无分组信息");
        }
        requestParam.setGidList(groupDoList.stream().map(GroupDo::getGid).toList());
        return shortRemoteLinkService.pageRecycleBinShortLink(requestParam);
    }
}
