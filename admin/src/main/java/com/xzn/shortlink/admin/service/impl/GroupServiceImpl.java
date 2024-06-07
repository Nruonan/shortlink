package com.xzn.shortlink.admin.service.impl;

import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.common.biz.user.UserContext;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.convention.exception.ServiceException;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.dao.entity.GroupDo;
import com.xzn.shortlink.admin.dao.entity.GroupUniqueDO;
import com.xzn.shortlink.admin.dao.mapper.GroupMapper;
import com.xzn.shortlink.admin.dao.mapper.GroupUniqueMapper;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.xzn.shortlink.admin.remote.dto.ShortLinkActualRemoteService;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkGroupQueryRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import com.xzn.shortlink.admin.toolkit.RandomGenerator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
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
    private final ShortLinkActualRemoteService shortLinkActualRemoteService;
    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;
    private final GroupUniqueMapper groupUniqueMapper;
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
            int retryCount = 0;
            int maxRetries = 10;
            String gid = null;
            // 如果布隆过滤器满了，可能会一直重复，所以这里加一个限制条件
            while(retryCount < maxRetries){
                // 生成随机gid
                gid = saveGroupUniqueReturnGid();
                if (StrUtil.isNotEmpty(gid)){
                    // 增加group
                    GroupDo group = GroupDo.builder()
                        .username(username)
                        .gid(gid)
                        .sortOrder(0)
                        .name(groupname)
                        .build();
                    baseMapper.insert(group);
                    gidRegisterCachePenetrationBloomFilter.add(gid);
                    break;
                }
                // 重试
                retryCount++;
            }
            // 确保在多次尝试后，如果gid仍然为空，说明无法生成有效的分组标识
            if (StrUtil.isEmpty(gid)){
                throw new ServiceException("生成分组标识频繁");
            }
        }finally {
            lock.unlock();
        }

    }

    private String saveGroupUniqueReturnGid() {
        String gid = RandomGenerator.generateRandom();
        // 如果布隆过滤器存在则返回空
        if (gidRegisterCachePenetrationBloomFilter.contains(gid)) {
            return null;
        }
        GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
            .gid(gid)
            .build();
        try {
            // 线程 A 和 B 同时生成了相同的 Gid，会被数据库的唯一索引校验触发异常
            // 流程不能被这个异常阻断，需要获取异常重试
            groupUniqueMapper.insert(groupUniqueDO);
        } catch (DuplicateKeyException e) {
            return null;
        }
        return gid;
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
        Result<List<ShortLinkGroupQueryRespDTO>> listResult = shortLinkActualRemoteService.listGroupShortLinkCount(
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
