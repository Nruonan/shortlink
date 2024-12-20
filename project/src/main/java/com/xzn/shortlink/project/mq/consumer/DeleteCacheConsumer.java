package com.xzn.shortlink.project.mq.consumer;

import com.xzn.shortlink.project.config.RabbitMQConfig;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Nruonan
 * @description
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
public class DeleteCacheConsumer {

        @Resource
        StringRedisTemplate stringRedisTemplate;
        /**
         * 消费消息
         * @param msg
         */
        @RabbitHandler
        public void deleteCache(String msg) {
            log.info("[消息访问统计监控] 消息：{}", msg);
            stringRedisTemplate.delete(msg);
        }
}
