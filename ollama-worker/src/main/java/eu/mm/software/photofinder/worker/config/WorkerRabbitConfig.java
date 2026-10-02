package eu.mm.software.photofinder.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerRabbitConfig {

    @Bean
    public Queue ollamaQueue() {
        return QueueBuilder.durable("ai.ollama.queue")
                .maxPriority(25)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange("ai.exchange");
    }

    @Bean
    public Binding ollamaBinding(Queue ollamaQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(ollamaQueue).to(aiExchange).with("ai.describe.ollama");
    }

    @Bean
    public MessageConverter jackson2MessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // Propagacja kontekstu trace do nagłówków wiadomości (Micrometer Observation).
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory workerListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(2);
        factory.setPrefetchCount(1);
        factory.setMessageConverter(messageConverter);
        // Odbiór kontekstu trace z nagłówków → span workera jako dziecko spanu apki.
        factory.setObservationEnabled(true);

        // Spójnie z główną apką (slowListenerFactory): 3 próby, potem reject → DLQ
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(5000, 2.0, 30000)
                .build());

        return factory;
    }
}
