package com.xzn.shortlink.project.service.impl;

import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.LOCK_GOTO_SHORT_LINK_KEY;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.project.common.convention.exception.ClientException;
import com.xzn.shortlink.project.common.convention.exception.ServiceException;
import com.xzn.shortlink.project.common.enums.VailDateTypeEnum;
import com.xzn.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.xzn.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.xzn.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.xzn.shortlink.project.dao.mapper.ShortLinkMapper;
import com.xzn.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkGroupQueryRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.project.service.ShortLinkService;
import com.xzn.shortlink.project.util.HashUtil;
import com.xzn.shortlink.project.util.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nruonan
 * @description 短链接接口实现层
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO>implements ShortLinkService {

    private final RBloomFilter<String> shortUriCachePenetrationBloomFilter;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        /*
            1、如果当前的短链接是没用过的，但是被误判用过了，那也没事，直接会被跳过，继续生成
            2、如果当前的短链接是用过的，但是被误判说没有用过，那么在插入到数据库的时候，就会被数据库的唯一字段，短链接拦截，出现异常
            3、如果在数据库没有拦截，说明没有误判, 获取到的就是没用过的，正常进行，不抛异常
        */
        // 根据原始连接生成后缀
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl =  requestParam.getDomain() + "/" +shortLinkSuffix;
        // 获取图标
        String favicon = getFavicon(requestParam.getOriginUrl());
        if(StrUtil.isEmpty(favicon)){
            favicon = "";
        }
        // 实例化ShortLinkDO
        ShortLinkDO shortLinkDO = BeanUtil.toBean(requestParam, ShortLinkDO.class);
        shortLinkDO.setFullShortUrl(requestParam.getDomain() + "/" +shortLinkSuffix);
        shortLinkDO.setEnableStatus(0);
        shortLinkDO.setShortUri(shortLinkSuffix);
        shortLinkDO.setFavicon(favicon);
        ShortLinkGotoDO shortLinkGoto = ShortLinkGotoDO.builder()
            .fullShortUrl(shortLinkDO.getFullShortUrl())
            .gid(requestParam.getGid())
            .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGoto);
        }catch (DuplicateKeyException ex){
            // 1.查数据库shortUrl
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            // 2.判断实例对象不为空
            if(hasShortLinkDO != null){
                log.warn("短链接：{}重复入库",fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        //布隆过滤器添加该短链接
        shortUriCachePenetrationBloomFilter.add(fullShortUrl);
        // 设置redis过期有效期
        stringRedisTemplate.opsForValue()
            .set(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidDate(requestParam.getValidDate()),TimeUnit.MILLISECONDS);
        return ShortLinkCreateRespDTO.builder()
            .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
            .originUrl(requestParam.getOriginUrl())
            .gid(requestParam.getGid())
            .build();
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 根绝请求体查询原有短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
            .eq(ShortLinkDO::getGid, requestParam.getGid())
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getEnableStatus, 0)
            .eq(ShortLinkDO::getDelFlag,0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        // 如果原有短链接不村子
        if(hasShortLinkDO == null){
            throw new ClientException("短链接记录不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
            .domain(hasShortLinkDO.getDomain())
            .shortUri(hasShortLinkDO.getShortUri())
            .clickNum(hasShortLinkDO.getClickNum())
            .favicon(hasShortLinkDO.getFavicon())
            .createdType(hasShortLinkDO.getCreatedType())
            .gid(requestParam.getGid())
            .originUrl(requestParam.getOriginUrl())
            .describe(requestParam.getDescribe())
            .validDateType(requestParam.getValidDateType())
            .validDate(requestParam.getValidDate())
            .build();
        // TODO 两个gid相同
        if(Objects.equals(hasShortLinkDO.getGid(),requestParam.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()),
                    ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO,updateWrapper);
        }else{
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(updateWrapper);
            baseMapper.insert(shortLinkDO);
        }
    }
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        // 查询分页
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
            .eq(ShortLinkDO::getGid, requestParam.getGid())
            .eq(ShortLinkDO::getEnableStatus, 0)
            .eq(ShortLinkDO::getDelFlag, 0)
            .orderByDesc(ShortLinkDO::getCreateTime);
        // 生成分页
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        // 封装分页
        return resultPage.convert(
            each -> {
                ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
                result.setDomain("http://" + result.getDomain());
                return result;
            });
    }

    @Override
    public List<ShortLinkGroupQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
            .select("gid as gid", "count(*) as shortLinkCount")
            .in("gid", requestParam)
            .eq("enable_status", 0)
            .groupBy("gid");
        List<Map<String,Object>> objects1 = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(objects1,ShortLinkGroupQueryRespDTO.class);
    }
    /**
     * 跳转短链接
     */
    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response)  {
        // 获取域名
        String serverName = request.getServerName();
        // 获取完整短链接
        String fullShortUrl = serverName + "/" + shortUri;
        // 从redis获取短链接
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(originalLink)){
            // 监控功能
            shortLinkStats(fullShortUrl,null,request,response);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        // 判断布隆过滤器是否有完整短链接
        boolean contains = shortUriCachePenetrationBloomFilter.contains(fullShortUrl);
        if(!contains){
            // 如果布隆过滤器没有则结束
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        // 判断redis是否存在该短链接为空的数据
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(gotoIsNullShortLink)){
            // 存在则结束
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        // 判断gotolink是否存在，
        if(StrUtil.isBlank(originalLink)){
            // 不存在 则加锁 b
            RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
            lock.lock();
            try {
                originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
                if(StrUtil.isNotBlank(originalLink)){
                    shortLinkStats(fullShortUrl,null,request,response);
                    ((HttpServletResponse) response).sendRedirect(originalLink);
                    return;
                }
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                // 得到跳转对象
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                if (shortLinkGotoDO == null) {
                    // TODO 需要封控
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30,
                        TimeUnit.MINUTES);
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                // 查询
                LambdaQueryWrapper<ShortLinkDO> linkQueryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0);
                // 获取短链接实体
                ShortLinkDO shortLinkDO = baseMapper.selectOne(linkQueryWrapper);
                // 如果短链接日期不存在或者过期了
                if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))) {
                    // 向redis设置该短链接不可访问
                    stringRedisTemplate.opsForValue()
                        .set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30,
                            TimeUnit.MINUTES);
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                // 重定向短链接
                ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
                shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
                // 向redis设置短链接，并添加过期时间
                stringRedisTemplate.opsForValue().set(
                    (String.format(GOTO_SHORT_LINK_KEY, fullShortUrl)),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate()),TimeUnit.MILLISECONDS);
            }finally{
                lock.unlock();
            }
        }
    }

    private void  shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response){
        try{
            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            int hour = DateUtil.hour(new Date(),true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                .pv(1)
                .uv(1)
                .uip(1)
                .hour(hour)
                .fullShortUrl(fullShortUrl)
                .weekday(weekValue)
                .gid(gid)
                .date(new Date())
                .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
        }catch (Throwable ex){
            log.error("短链接访问统计异常",ex);
        }
    }
    /**
     * 获取网站图标
     */
    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return  null;
    }
    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        // 定义重复次数
        int customGenerateCount = 0;
        String shortUri;
        while(true){
            if(customGenerateCount > 10){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            // 获取原始域名
            String originUrl = requestParam.getOriginUrl();
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            // 用布隆过滤器判断uri是否已经存在
            if(!shortUriCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }
}
