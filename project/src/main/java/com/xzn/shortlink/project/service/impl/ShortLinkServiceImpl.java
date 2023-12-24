package com.xzn.shortlink.project.service.impl;

import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.GOTO_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.RedisConstantKey.LOCK_GOTO_SHORT_LINK_KEY;
import static com.xzn.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.project.common.convention.exception.ClientException;
import com.xzn.shortlink.project.common.convention.exception.ServiceException;
import com.xzn.shortlink.project.common.enums.VailDateTypeEnum;
import com.xzn.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.xzn.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkBrowserStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkOsStatsDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.xzn.shortlink.project.dao.mapper.LinkAccessLogsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkBrowserStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkOsStatsMapper;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.redisson.api.RedissonClient;
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
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

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
                // 监控短链接
                shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
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

    private void  shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response){
        // 获取cookie
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        // 判断是否利用cookie访问
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        try{
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
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" +fullShortUrl, uv.get());
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
                        Long add = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl,each);
                        // 添加成功应该大于0，不为null，否则为false
                        uvFirstFlag.set(add != null && add > 0L);
                    },addResponseCookieTask);
            }else{
                // cookie为空，则执行上列语句
                addResponseCookieTask.run();
            }
            // 获取用户访问的ip
            String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
            // 添加到redis
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
            // 判断是否重复加入
            boolean uipFirstFlag = uipAdded != null && uipAdded >0;
            // 判断gid是否为null
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
                // 如果为true，则第一次访问，后续则为0
                .uv(uvFirstFlag.get() ? 1 : 0)
                .uip(uipFirstFlag ? 1 : 0)
                .hour(hour)
                .fullShortUrl(fullShortUrl)
                .weekday(weekValue)
                .gid(gid)
                .date(new Date())
                .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);

            Map<String,Object> localeMap = new HashMap<>();
            // 设置高德key和ip
            localeMap.put("key",statsLocaleAmapKey);
            localeMap.put("ip",remoteAddr);
            // 访问高德ip定位的api
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeMap);
            // 获取IPJson格式对象
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            // 得到信息状态码
            String infocode = localeResultObj.getString("infocode");
            // 判断获取是否成功
            if(StrUtil.isNotBlank(infocode) && Objects.equals(infocode,"10000")){
                // 如果成功，获取省份的
                String province = localeResultObj.getString("province");
                // 判断是否为大括号
                boolean unknownFlag = StrUtil.equals(province,"[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                    .province(unknownFlag ? province : "未知")
                    .city(unknownFlag ? localeResultObj.getString("city") : "未知")
                    .adcode(unknownFlag ? localeResultObj.getString("adcode") : "未知")
                    .country("中国")
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                // 插入国家地区数据
                linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO);

                // 插入操作系统数据
                String os = LinkUtil.getOs((HttpServletRequest) request);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(os)
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);

                // 插入浏览器数据
                String browser = LinkUtil.getBrowser((HttpServletRequest) request);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(browser)
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);

                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .ip(remoteAddr)
                    .os(os)
                    .browser(browser)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .user(uv.get())
                    .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);
            }

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
