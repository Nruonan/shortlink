package com.xzn.shortlink.project.service.impl;


import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_SHORT_LINK_KEY;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dao.mapper.ShortLinkMapper;
import com.xzn.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.xzn.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.xzn.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.project.service.RecycleBinService;
import com.xzn.shortlink.project.util.LinkUtil;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description 回收站管理接口实现层
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO>implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        // 查询原有的短链接
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getGid, requestParam.getGid())
            .eq(ShortLinkDO::getEnableStatus, 0)
            .eq(ShortLinkDO::getDelFlag, 0);
        // 更改短链接的使用状态为1
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                    .build();
        baseMapper.update(shortLinkDO,updateWrapper);
        // 删除redis的缓存
        stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        // 查询分页
        IPage<ShortLinkDO> resultPage = baseMapper.pageRecycleBinLink(requestParam);
        // 封装分页
        return resultPage.convert(
            each -> {
                ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
                result.setDomain("http://" + result.getDomain());
                return result;
            });
    }

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        // 查询回收站的短链接
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getGid, requestParam.getGid())
            .eq(ShortLinkDO::getEnableStatus, 1)
            .eq(ShortLinkDO::getDelFlag, 0);
        // 更改短链接的使用状态为0
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
            .enableStatus(0)
            .build();
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(updateWrapper);
        baseMapper.update(shortLinkDO,updateWrapper);
        // 删除空缓存
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
        // 添加redis的缓存
        stringRedisTemplate.opsForValue().set((String.format(GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl())),hasShortLinkDO.getOriginUrl(),
            LinkUtil.getLinkCacheValidDate(hasShortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
    }

    @Override
    public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
        // 查询原有的短链接
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getGid, requestParam.getGid())
            .eq(ShortLinkDO::getEnableStatus, 1)
            .eq(ShortLinkDO::getDelTime, 0L)
            .eq(ShortLinkDO::getDelFlag, 0);
        // 更改短链接的使用状态为1
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .delTime(System.currentTimeMillis())
                    .build();
        shortLinkDO.setDelFlag(1);
        baseMapper.update(shortLinkDO,updateWrapper);
    }
}
