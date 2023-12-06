package com.xzn.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.biz.user.UserContext;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import com.xzn.shortlink.admin.toolkit.RandomGenerator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description
 */
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDo> implements GroupService {

    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        String gid;
        do{
            gid = RandomGenerator.generateRandom();
        }while(!hasGid(gid));
        GroupDo groupDo = GroupDo.builder()
                    .gid(gid)
                    .sortOrder(0)
                    .username(UserContext.getUsername())
                    .name(requestParam.getName())
                    .build();
        baseMapper.insert(groupDo);
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getDelFlag, 0)
            // TODO 设置用户名
            .eq(GroupDo::getUsername, UserContext.getUsername())
            .orderByDesc(GroupDo::getSortOrder, GroupDo::getUpdateTime);
        List<GroupDo> groupDos = baseMapper.selectList(queryWrapper);
        return BeanUtil.copyToList(groupDos, ShortLinkGroupRespDTO.class);
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getGid,gid)
            .eq(GroupDo::getUsername, UserContext.getUsername());
        GroupDo groupDo = baseMapper.selectOne(queryWrapper);
        return groupDo == null;
    }
}
