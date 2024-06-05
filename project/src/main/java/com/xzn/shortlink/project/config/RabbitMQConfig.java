package com.xzn.shortlink.project.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String QUEUE = "short-link_project-service_queue";
    public static final String EXCHANGE = "short-link_project-service_exchange";
    public static String DLX_ROUTING_KEY = "short-link_project-service_key";
    /**
     * 1. 配置队列
     * 2. 队列名为 queue
     * 3. true 表示: 持久化
     * durable： 队列是否持久化。 队列默认是存放到内存中的，rabbitmq 重启则丢失，
     * 若想重启之后还存在则队列要持久化，
     * 保存到 Erlang 自带的 Mnesia 数据库中，当 rabbitmq 重启之后会读取该数据库
     */
    @Bean
    public Queue nQueue() {
        return new Queue(QUEUE);
    }

    /**
     * 死信交换机
     */
    @Bean
    DirectExchange Exchange() {
        return new DirectExchange(EXCHANGE);
    }
    /**
     * 绑定死信队列和死信交换机
     */
    @Bean
    Binding dlxBinding() {
        return BindingBuilder.bind(nQueue()).to(Exchange())
            .with(DLX_ROUTING_KEY);
    }



}