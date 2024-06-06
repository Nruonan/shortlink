package com.xzn.shortlink.project.mq.consumer;

import static com.xzn.shortlink.project.common.constant.RedisConstantKey.LOCK_GID_UPDATE_KEY;
import static com.xzn.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xzn.shortlink.project.common.convention.exception.ServiceException;
import com.xzn.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.xzn.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkBrowserStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkDeviceStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkNetworkStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkOsStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkStatsTodayDO;
import com.xzn.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.xzn.shortlink.project.dao.mapper.LinkAccessLogsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkBrowserStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkDeviceStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkNetworkStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkOsStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkStatsTodayMapper;
import com.xzn.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.xzn.shortlink.project.dao.mapper.ShortLinkMapper;
import com.xzn.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.xzn.shortlink.project.dto.biz.ShortLinkStatsRecordGroupDTO;
import com.xzn.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import com.xzn.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Nruonan
 * @description
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(
    queues = "short-link_project-service_queue"
)
public class ShortLinkStatsSaveConsumer{

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmqpKey;


    @RabbitHandler
    public void onMessage(String msg) {
        ShortLinkStatsRecordGroupDTO record = JSON.parseObject(msg,ShortLinkStatsRecordGroupDTO.class);
        String keys = record.getKey();
        if (messageQueueIdempotentHandler.isMessageBeingConsumed(keys)) {
            // 判断当前的这个消息流程是否执行完成
            if (messageQueueIdempotentHandler.isAccomplish(keys)) {
               return;
            }
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }
        try {
            String fullShortUrl = record.getFullShortUrl();
            if (StrUtil.isNotBlank(fullShortUrl)) {
                String gid = record.getGid();
                ShortLinkStatsRecordDTO statsRecord = ShortLinkStatsRecordDTO.builder()
                    .os(record.getOs())
                    .remoteAddr(record.getRemoteAddr())
                    .browser(record.getBrowser())
                    .uipFirstFlag(record.getUipFirstFlag())
                    .uvFirstFlag(record.getUvFirstFlag())
                    .network(record.getNetwork())
                    .uv(record.getUv())
                    .device(record.getDevice()).build();
                actualSaveShortLinkStats(gid, fullShortUrl, statsRecord);
            }
        } catch (Throwable ex) {
            log.error("记录短链接监控消费异常", ex);
            throw ex;
        }
        messageQueueIdempotentHandler.setAccomplish(keys);


    }

    private void actualSaveShortLinkStats(String gid, String fullShortUrl, ShortLinkStatsRecordDTO statsRecord) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        if (!rLock.tryLock()) {
            // 延迟队列发送
            delayShortLinkStatsProducer.send(statsRecord);
            return;
        }
        try{
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
                .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                .hour(hour)
                .fullShortUrl(fullShortUrl)
                .weekday(weekValue)
                .gid(gid)
                .date(new Date())
                .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);

            Map<String,Object> localeMap = new HashMap<>();
            // 设置高德key和ip
            localeMap.put("key",statsLocaleAmqpKey);
            localeMap.put("ip",statsRecord.getRemoteAddr());
            // 访问高德ip定位的api
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeMap);
            // 获取IPJson格式对象
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            // 得到信息状态码
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince = "未知";
            String actualCity = "未知";
            // 判断获取是否成功
            if(StrUtil.isNotBlank(infoCode) && Objects.equals(infoCode,"10000")){
                // 如果成功，获取省份的
                String province = localeResultObj.getString("province");
                // 判断是否为大括号
                boolean unknownFlag = StrUtil.equals(province,"[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                    .province(actualProvince = unknownFlag ? actualProvince : province)
                    .city(actualCity = unknownFlag ? actualCity : localeResultObj.getString("city"))
                    .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                    .country("中国")
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                // 插入国家地区数据
                linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO);

                // 插入操作系统数据
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(statsRecord.getOs())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);

                // 插入浏览器数据
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);


                // 插入设备数据
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(statsRecord.getDevice())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkDeviceStatsMapper.shortLinkDeviceStats(linkDeviceStatsDO);

                // 插入网络数据
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(statsRecord.getNetwork())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
                linkNetworkStatsMapper.shortLinkNetworkStats(linkNetworkStatsDO);

                // 插入监控日志
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(statsRecord.getUv())
                    .ip(statsRecord.getRemoteAddr())
                    .os(statsRecord.getOs())
                    .browser(statsRecord.getBrowser())
                    .network(statsRecord.getNetwork())
                    .device(statsRecord.getDevice())
                    .locale(StrUtil.join("-","中国" , actualProvince , actualCity))
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);
                // 添加总计uv等数据到shortLink
                shortLinkMapper.incrementStats(gid,fullShortUrl,1,statsRecord.getUvFirstFlag() ? 1: 0,statsRecord.getUipFirstFlag() ? 1 : 0);
                // 添加单日uv等数据到shortLink_today
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .todayPv(1)
                    .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .date(new Date())
                    .build();
                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
            }

        }catch (Throwable ex){
            log.error("短链接访问统计异常",ex);
        }finally {
            rLock.unlock();
        }
    }



}
