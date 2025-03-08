package com.xzn.shortlink.project.service.impl;

import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.LOCK_GID_UPDATE_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.LOCK_GOTO_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.SHORT_LINK_CREATE_LOCK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.SHORT_LINK_STATS_UIP_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.SHORT_LINK_STATS_UV_KEY;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.project.common.convention.exception.ClientException;
import com.xzn.shortlink.project.common.convention.exception.ServiceException;
import com.xzn.shortlink.project.common.enums.VailDateTypeEnum;
import com.xzn.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.xzn.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.xzn.shortlink.project.dao.mapper.ShortLinkMapper;
import com.xzn.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkBaseInfoRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkGroupQueryRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.xzn.shortlink.project.mq.idempotent.ShortLinkStatsRecordListenerDTO;
import com.xzn.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.xzn.shortlink.project.service.ShortLinkService;
import com.xzn.shortlink.project.util.HashUtil;
import com.xzn.shortlink.project.util.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    private final RabbitTemplate rabbitTemplate;

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        /*
            1、如果当前的短链接是没用过的，但是被误判用过了，那也没事，直接会被跳过，继续生成
            2、如果当前的短链接是用过的，但是被误判说没有用过，那么在插入到数据库的时候，就会被数据库的唯一字段，短链接拦截，出现异常
            3、如果在数据库没有拦截，说明没有误判, 获取到的就是没用过的，正常进行，不抛异常
        */
        verificationWhitelist(requestParam.getOriginUrl());
        // 根据原始连接生成后缀
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl =  StrBuilder.create(createShortLinkDefaultDomain) + "/" +shortLinkSuffix;
        // 获取图标
        String favicon = getFavicon(requestParam.getOriginUrl());
        if (favicon != null && favicon.length() == 0) {
            favicon = "";
        }
        // 实例化ShortLinkDO
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
            .domain(createShortLinkDefaultDomain)
            .originUrl(requestParam.getOriginUrl())
            .gid(requestParam.getGid())
            .createdType(requestParam.getCreatedType())
            .validDateType(requestParam.getValidDateType())
            .validDate(requestParam.getValidDate())
            .describe(requestParam.getDescribe())
            .shortUri(shortLinkSuffix)
            .enableStatus(0)
            .totalPv(0)
            .totalUv(0)
            .totalUip(0)
            .fullShortUrl(fullShortUrl)
            .favicon(getFavicon(requestParam.getOriginUrl()))
            .build();
        ShortLinkGotoDO shortLinkGoto = ShortLinkGotoDO.builder()
            .fullShortUrl(shortLinkDO.getFullShortUrl())
            .gid(requestParam.getGid())
            .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGoto);
        }catch (DuplicateKeyException ex){
            // 首先判断是否存在布隆过滤器，如果不存在直接新增
            if (!shortUriCachePenetrationBloomFilter.contains(fullShortUrl)) {
                shortUriCachePenetrationBloomFilter.add(fullShortUrl);
            }
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
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
    public ShortLinkCreateRespDTO createShortLinkByLock(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        String fullShortUrl;
        RLock lock = redissonClient.getLock(SHORT_LINK_CREATE_LOCK_KEY);
        lock.lock();
        try{
            String shortLinkSuffix = generateSuffix(requestParam);
            fullShortUrl =  StrBuilder.create(createShortLinkDefaultDomain) + "/" +shortLinkSuffix;
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .fullShortUrl(fullShortUrl)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .build();
            ShortLinkGotoDO shortLinkGoto = ShortLinkGotoDO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .gid(requestParam.getGid())
                .build();
            try {
                baseMapper.insert(shortLinkDO);
                shortLinkGotoMapper.insert(shortLinkGoto);
            }catch (DuplicateKeyException e){
                throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
            }
            // 设置redis过期有效期
            stringRedisTemplate.opsForValue()
                .set(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl),
                    requestParam.getOriginUrl(),
                    LinkUtil.getLinkCacheValidDate(requestParam.getValidDate()),TimeUnit.MILLISECONDS);
        }finally {
            lock.unlock();
        }
        return ShortLinkCreateRespDTO.builder()
            .fullShortUrl("http://" + fullShortUrl)
            .originUrl(requestParam.getOriginUrl())
            .gid(requestParam.getGid())
            .build();
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        // 获取集合中原始地址和描述
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        ArrayList<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++){
            // 获取单个短连接实体
            ShortLinkCreateReqDTO bean = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            bean.setOriginUrl(originUrls.get(i));
            bean.setDescribe(describes.get(i));
            try{
                // 创建单个短连接
                ShortLinkCreateRespDTO shortLink = createShortLink(bean);
                // 创建相应实体
                ShortLinkBaseInfoRespDTO shortLinkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                    .fullShortUrl(shortLink.getFullShortUrl())
                    .originUrl(shortLink.getOriginUrl())
                    .describe(describes.get(i))
                    .build();
                // 将单个短连接重要信息添加链表里
                result.add(shortLinkBaseInfoRespDTO);
            }catch (Throwable ex){
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
            .baseLinkInfos(result)
            .total(result.size()) // 成功个数
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        // 根绝请求体查询原有短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
            .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getDelFlag, 0)
            .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        // 如果原有短链接不存在
        if(hasShortLinkDO == null){
            throw new ClientException("短链接记录不存在");
        }
        //两个gid相同 替换就的短连接
        if(Objects.equals(hasShortLinkDO.getGid(),requestParam.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()),
                    ShortLinkDO::getValidDate, null);
            // 创建短连接新对象
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .fullShortUrl(requestParam.getFullShortUrl())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
            baseMapper.update(shortLinkDO,updateWrapper);
        }else{
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            rLock.lock();
            try {
                // 先查老分组的短连接
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getDelTime, 0L)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(ShortLinkDO::getDelFlag,1)
                    .set(ShortLinkDO::getDelTime,System.currentTimeMillis());
                // 将时间改了
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                    .delTime(System.currentTimeMillis())
                    .build();
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                // 添加新的短连接
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(createShortLinkDefaultDomain)
                    .originUrl(requestParam.getOriginUrl())
                    .gid(requestParam.getGid())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .describe(requestParam.getDescribe())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .enableStatus(hasShortLinkDO.getEnableStatus())
                    .totalPv(hasShortLinkDO.getTotalPv())
                    .totalUv(hasShortLinkDO.getTotalUv())
                    .totalUip(hasShortLinkDO.getTotalUip())
                    .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                    .favicon(getFavicon(requestParam.getOriginUrl()))
                    .delTime(0L)
                    .build();
                baseMapper.insert(shortLinkDO);
                // 先删goto表中的gid和完整短连接数据
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, hasShortLinkDO.getFullShortUrl())
                    .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.delete(linkGotoQueryWrapper);
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
            } finally {
                rLock.unlock();
            }

        }

            // 判断时间是否有效
            if (!Objects.equals(requestParam.getValidDateType(),VailDateTypeEnum.PERMANENT.getType())
                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())
                || !Objects.equals(hasShortLinkDO.getOriginUrl(), requestParam.getOriginUrl())
                ){
                // 先删除原来短连接的缓存 防止访问错误网址
                stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
                rabbitTemplate.convertAndSend("short-link_project-normal.exchange", "short-link_project-normal.key", String.format(GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()), correlationData->{
                    correlationData.getMessageProperties().setExpiration("3000");
                    return correlationData;
                });
                Date currentDate = new Date();
                if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(currentDate)) {
                    if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(currentDate)) {
                        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                    }
                }
            }

    }
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
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
        String serverPort = Optional.of(request.getServerPort())
            .filter(each -> !Objects.equals(each, 80))
            .map(String::valueOf)
            .map(each -> ":" + each)
            .orElse("");
        // 获取完整短链接
        String fullShortUrl = serverName + serverPort +  "/" + shortUri;
        // 从redis获取短链接
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(originalLink)){
            // 监控功能
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl,null,statsRecord);
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
                // 从redis中获取短链接数据
                originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
                if(StrUtil.isNotBlank(originalLink)){
                    // 跳转短链接
                    ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                    // 统计数据
                    shortLinkStats(fullShortUrl,null,statsRecord);
                    ((HttpServletResponse) response).sendRedirect(originalLink);
                    return;
                }
                // 如果为空值表
                gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl));
                if (StrUtil.isNotBlank(gotoIsNullShortLink)){
                    // 跳转notfound页面
                    ((HttpServletResponse)response).sendRedirect("/page/notfound");
                    return;
                }
                // 查询goto表
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                // 得到跳转对象
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                // 如果对象不存在这存入空值
                if (shortLinkGotoDO == null) {
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30,
                        TimeUnit.MINUTES);
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                // 查询shortlink表
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
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                // 监控短链接
                shortLinkStats(fullShortUrl,shortLinkDO.getGid(),statsRecord);
                // 重定向短链接
                ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
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
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        // 获取cookie
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        // 判断是否利用cookie访问
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        AtomicReference<String> uv = new AtomicReference<>();
        // 添加cookie
        Runnable addResponseCookieTask = () ->{
            // 生成uv的UUID
            uv.set(UUID.fastUUID().toString());
            // 生成一个cookie
            Cookie uvCookie = new Cookie("uv", uv.get());
            // 设置cookie的时间
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            // 设置cookie作用域 （短链接）
            uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf("/"),fullShortUrl.length()));
            // 往响应体添加cookie
            ((HttpServletResponse)response).addCookie(uvCookie);
            // 设置为初始利用cookie访问
            uvFirstFlag.set(Boolean.TRUE);
            // 往redis添加uv
            stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY +fullShortUrl, uv.get());
        };
        if(ArrayUtil.isNotEmpty(cookies)){
            // 如果cookie不为空
            Arrays.stream(cookies)
                // 查找uv的cookie值
                .filter(each -> Objects.equals(each.getName(),"uv"))
                .findFirst()
                .map(Cookie::getValue)
                .ifPresentOrElse(each ->{
                    uv.set(each);
                    // 尝试利用set数据结构添加
                    Long add = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl,each);
                    // 添加成功应该大于0，不为null，否则为false
                    uvFirstFlag.set(add != null && add > 0L);
                },addResponseCookieTask);
        }else{
            // cookie为空，则执行上列语句
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
            .fullShortUrl(fullShortUrl)
            .uv(uv.get())
            .uvFirstFlag(uvFirstFlag.get())
            .uipFirstFlag(uipFirstFlag)
            .remoteAddr(remoteAddr)
            .os(os)
            .browser(browser)
            .device(device)
            .network(network)
            .build();
    }
    @Override
    public void  shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord){
        ShortLinkStatsRecordListenerDTO recordGroupDTO = ShortLinkStatsRecordListenerDTO.builder()
            .os(statsRecord.getOs())
            .remoteAddr(statsRecord.getRemoteAddr())
            .browser(statsRecord.getBrowser())
            .uipFirstFlag(statsRecord.getUipFirstFlag())
            .uvFirstFlag(statsRecord.getUvFirstFlag())
            .fullShortUrl(fullShortUrl)
            .network(statsRecord.getNetwork())
            .uv(statsRecord.getUv())
            .device(statsRecord.getDevice())
            .gid(gid).build();
        shortLinkStatsSaveProducer.send( JSON.toJSONString(recordGroupDTO));
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
            originUrl += UUID.randomUUID().toString();
            shortUri = HashUtil.hashToBase62(originUrl);
            // 用布隆过滤器判断uri是否已经存在
            if(!shortUriCachePenetrationBloomFilter.contains(createShortLinkDefaultDomain + "/" + shortUri)){
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }

    public void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable){
            throw new ClientException("跳转链接填写错误");
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }
}
