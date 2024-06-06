package com.xzn.shortlink.admin.service.impl;

import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.common.biz.user.UserContext;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.xzn.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkGroupQueryRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import com.xzn.shortlink.admin.toolkit.RandomGenerator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDo> implements GroupService {
    /**
     * 后续重构为 springcloud
     */
    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;
    private final RedissonClient redissonClient;

    ShortLinkRemoteService shortRemoteLinkService = new ShortLinkRemoteService(){};
    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        saveGroup(UserContext.getUsername(),requestParam.getName());
    }

    @Override
    public void saveGroup(String username, String groupname) {
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY,groupname));
        // 上锁
        lock.lock();
        try{
            // 查询用户的所有分组
            LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
                .eq(GroupDo::getUsername,username)
                .eq(GroupDo::getDelFlag,0);
            List<GroupDo> groupDos = baseMapper.selectList(queryWrapper);
            // 若达到20个，就不许创建
            if (CollUtil.isNotEmpty(groupDos) && groupDos.size() == groupMaxNum){
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            String gid;
            do{
                gid = RandomGenerator.generateRandom();
            }while(!hasGid(username,gid));
            GroupDo groupDo = GroupDo.builder()
                .gid(gid)
                .sortOrder(0)
                .username(username)
                .name(groupname)
                .build();
            baseMapper.insert(groupDo);
        }finally {
            lock.unlock();
        }

    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // 先查组
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getDelFlag, 0)
            .eq(GroupDo::getUsername, UserContext.getUsername())
            .orderByDesc(GroupDo::getSortOrder, GroupDo::getUpdateTime);
        List<GroupDo> groupDos = baseMapper.selectList(queryWrapper);
        // 通过gid远程调用获取短链接个数
        Result<List<ShortLinkGroupQueryRespDTO>> listResult = shortRemoteLinkService.listGroupShortLinkCount(
            groupDos.stream().map(GroupDo::getGid).toList());
        // 封装到响应对象
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDos, ShortLinkGroupRespDTO.class);
        // 把数量设置到shortLinkGroupRespDTOList
        shortLinkGroupRespDTOList.forEach(each ->{
            String gid = each.getGid();
            Optional<ShortLinkGroupQueryRespDTO> first = listResult.getData().stream()
                .filter(item -> Objects.equals(item.getGid(), each.getGid()))
                .findFirst();
            first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
        });
        return shortLinkGroupRespDTOList;
    }

    private boolean hasGid(String username , String gid){
        LambdaQueryWrapper<GroupDo> queryWrapper = Wrappers.lambdaQuery(GroupDo.class)
            .eq(GroupDo::getGid,gid)
            .eq(GroupDo::getUsername, Optional.ofNullable(username).orElse(UserContext.getUsername()));
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
        // TODO 删除分组后把分组下的短链接丢到回收站
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
