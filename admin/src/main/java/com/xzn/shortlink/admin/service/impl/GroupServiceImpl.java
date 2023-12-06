package com.xzn.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.biz.user.UserContext;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
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

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getUsername, UserContext.getUsername())
            .eq(GroupDo::getGid, requestParam.getGid())
            .eq(GroupDo::getDelFlag, 0);
        GroupDo groupDo = new GroupDo();
        groupDo.setName(requestParam.getName());
        baseMapper.update(groupDo,queryWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDo> updateWrapper = Wrappers.lambdaUpdate(GroupDo.class)
            .eq(GroupDo::getGid,gid)
            .eq(GroupDo::getUsername, UserContext.getUsername());
        GroupDo groupDo = baseMapper.selectOne(updateWrapper);
        groupDo.setDelFlag(1);
        baseMapper.update(groupDo,updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(each->{
            GroupDo groupDo = GroupDo.builder()
                .gid(each.getGid())
                .sortOrder(each.getSortOrder())
                .build();
            LambdaUpdateWrapper<GroupDo> updateWrapper = Wrappers.lambdaUpdate(GroupDo.class)
                .eq(GroupDo::getGid, each.getGid())
                .eq(GroupDo::getUsername, UserContext.getUsername())
                .eq(GroupDo::getDelFlag, 0);
            baseMapper.update(groupDo,updateWrapper);
        });
    }
}
