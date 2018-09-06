package ru.vvizard.rssspy.botservice.mq;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by igormazevich
 * Конфигурационный файл для RabbitMQ
 */

@Configuration
@EnableRabbit
public class RabbitConfiguration {

    public static final String QUEUE_ADD_CHAT_TO_FEED ="addChatToFeed";
    public static final String QUEUE_UPDATE_FEED ="updateFeed";
    public static final String QUEUE_DEL_RSS ="delRss";
    public static final String QUEUE_SEND_FEED_MSG ="sendFeed";
    public static final String QUEUE_GANALYTICS_MSG ="ganalyticsMsg";
    public static final String QUEUE_CHECK_FEED ="checkFeed";

    @Value("${MQ_SERVER}")
    private String mq_server;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    @Bean
    public ConnectionFactory connectionFactory(){
        return new CachingConnectionFactory(mq_server);
    }

    @Bean
    public AmqpAdmin amqpAdmin(){
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(){
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public Queue addChatToFeed(){
        return new Queue(QUEUE_ADD_CHAT_TO_FEED);
    }

    @Bean
    public Queue updateFeed(){
        return new Queue(QUEUE_UPDATE_FEED);
    }

    @Bean
    public Queue delRss(){
        return new Queue(QUEUE_DEL_RSS);
    }

    @Bean
    public Queue sendFeedMsg(){
        return new Queue(QUEUE_SEND_FEED_MSG);
    }

    @Bean
    public Queue sendGAMsg(){
        return new Queue(QUEUE_GANALYTICS_MSG);
    }

    @Bean
    public Queue checkFeed(){
        return new Queue(QUEUE_CHECK_FEED);
    }
}


