package com.xzn.shortlink.project.mq.producer;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.xzn.shortlink.project.config.RabbitMQConfig;
import com.xzn.shortlink.project.dto.biz.ShortLinkStatsRecordGroupDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
/**
 * @author Nruonan
 * 短链接监控状态保存消息队列生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private String exchange = RabbitMQConfig.EXCHANGE;

    /**
     * 发送延迟消费短链接统计
     */
    public void send(String msg) {
        String key = String.valueOf(UUID.randomUUID());
        ShortLinkStatsRecordGroupDTO shortLinkStatsRecordGroupDTO = JSON.parseObject(msg,
            ShortLinkStatsRecordGroupDTO.class);
        shortLinkStatsRecordGroupDTO.setKey(key);
        msg = JSON.toJSONString(shortLinkStatsRecordGroupDTO);

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,RabbitMQConfig.DLX_ROUTING_KEY , msg, msga -> {
                msga.getMessageProperties().setExpiration(1000*5+"");
                return msga;
            });
            log.info("[消息访问统计监控] 消息keys:{},消息：{}",  key,msg);
        } catch (Throwable ex) {
            log.error("[消息访问统计监控] 消息发送失败，消息体：{}", JSON.toJSONString(msg), ex);
            // 自定义行为...
        }
    }
}
