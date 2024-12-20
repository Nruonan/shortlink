package com.xzn.shortlink.project.config;


import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String QUEUE = "short-link_project-service_queue";
    public static final String EXCHANGE = "short-link_project-service_exchange";
    public static String ROUTING_KEY = "short-link_project-service_key";
    // 普通交换机名称
    public static final String NORMAL_EXCHANGE = "short-link_project-normal.exchange";
    // 死信交换机名称
    public static final String DEAD_LETTER_EXCHANGE = "short-link_project-dead.letter.exchange";
    // 普通队列名称
    public static final String NORMAL_QUEUE = "short-link_project-normal.queue";
    // 死信队列名称
    public static final String DEAD_LETTER_QUEUE = "short-link_project-dead.letter.queue";
    // 普通路由键
    public static final String NORMAL_ROUTING_KEY = "short-link_project-normal.key";
    // 死信路由键
    public static final String DEAD_LETTER_ROUTING_KEY = "short-link_project-dead.letter.key";

    // 声明普通交换机
    @Bean
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE);
    }

    // 声明死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // 声明普通队列，并设置死信交换机参数
    @Bean
    public Queue normalQueue() {
        Map<String, Object> args = new HashMap<>(3);
        // 设置死信交换机
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        // 设置死信路由键
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        return QueueBuilder.durable(NORMAL_QUEUE)
            .withArguments(args)
            .build();
    }

    // 声明死信队列
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE);
    }

    // 绑定普通交换机和普通队列
    @Bean
    public Binding normalBinding() {
        return BindingBuilder.bind(normalQueue())
            .to(normalExchange())
            .with(NORMAL_ROUTING_KEY);
    }

    // 绑定死信交换机和死信队列
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with(DEAD_LETTER_ROUTING_KEY);
    }
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
            .with(ROUTING_KEY);
    }




}