package com.xzn.shortlink.project.mq.producer;

import static com.xzn.shortlink.project.config.RabbitMQConfig.EXCHANGE;
import static com.xzn.shortlink.project.config.RabbitMQConfig.ROUTING_KEY;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.xzn.shortlink.project.mq.idempotent.ShortLinkStatsRecordListenerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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




    /**
     * 发送延迟消费短链接统计
     */
    public void send(String msg) {
        String key = String.valueOf(UUID.randomUUID());
        ShortLinkStatsRecordListenerDTO shortLinkStatsRecordListenerDTO = JSON.parseObject(msg,
            ShortLinkStatsRecordListenerDTO.class);
        shortLinkStatsRecordListenerDTO.setKey(key);
        msg = JSON.toJSONString(shortLinkStatsRecordListenerDTO);

        try {
            rabbitTemplate.convertAndSend(EXCHANGE,ROUTING_KEY , msg, msga -> {
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
