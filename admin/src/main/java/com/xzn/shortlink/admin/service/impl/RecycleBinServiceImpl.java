package com.xzn.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzn.shortlink.admin.common.biz.user.UserContext;
import com.xzn.shortlink.admin.common.convention.exception.ServiceException;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.remote.dto.ShortLinkActualRemoteService;
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
    private final ShortLinkActualRemoteService shortLinkActualRemoteService;
    @Override
    public Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getUsername, UserContext.getUsername())
            .eq(GroupDo::getDelFlag, 0);
        List<GroupDo> groupDoList = groupMapper.selectList(queryWrapper);
        if(CollUtil.isEmpty(groupDoList)){
            throw new ServiceException("用户无分组信息");
        }
        requestParam.setGidList(groupDoList.stream().map(GroupDo::getGid).toList());
        return shortLinkActualRemoteService.pageRecycleBinShortLink(requestParam.getGidList(),requestParam.getCurrent(),requestParam.getSize());
    }
}
