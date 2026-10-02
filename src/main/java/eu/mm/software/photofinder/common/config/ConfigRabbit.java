package eu.mm.software.photofinder.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.domain.ImageNotFoundException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Configuration
public class ConfigRabbit {

    private static final int MAX_PRIORITY = 25;

    public static final String EMBEDDED = "embedded";

    @Bean
    public SimpleRabbitListenerContainerFactory persistenceListenerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jackson2MessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setConcurrentConsumers(16);
        factory.setMaxConcurrentConsumers(32);
        factory.setPrefetchCount(64);

        factory.setMessageConverter(jackson2MessageConverter);
        factory.setObservationEnabled(true);

        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(new SimpleRetryPolicy(5,
                        Map.of(MongoException.class, true)))
                .backOffOptions(1000, 2.0, 16000)
                .build());

        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitMultiCoreListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jackson2MessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setConcurrentConsumers(32);
        factory.setMaxConcurrentConsumers(64);

        factory.setPrefetchCount(64);

        factory.setMessageConverter(jackson2MessageConverter);
        factory.setObservationEnabled(true);

        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(new SimpleRetryPolicy(5,
                        Collections.singletonMap(HttpClientErrorException.TooManyRequests.class, true)))
                .backOffOptions(2000, 2.0, 30000)
                .build());

        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory slowListenerFactory(ConnectionFactory connectionFactory,
                                                                    MessageConverter jackson2MessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setMessageConverter(jackson2MessageConverter);
        factory.setObservationEnabled(true);
        factory.setDefaultRequeueRejected(false);

        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(slowListenerRetryPolicy())
                .backOffOptions(5000, 2.0, 30000)
                .build());

        return factory;
    }

    /**
     * Polityka retry dla ścieżki opisu (slowListenerFactory): ponawiaj 3× domyślnie,
     * ale NIE dla brakującej miniatury — to błąd trwały, ma trafić od razu do DLQ
     * (bez ~15 s marnowanych na beznadziejne retry). traverseCauses=true, bo
     * ImageNotFoundException jest opakowany w AmqpRejectAndDontRequeueException.
     */
    static SimpleRetryPolicy slowListenerRetryPolicy() {
        return new SimpleRetryPolicy(3,
                Map.of(
                        AmqpRejectAndDontRequeueException.class, false,
                        ImageNotFoundException.class, false
                ),
                true,   // traverseCauses
                true);  // domyślnie ponawiaj pozostałe wyjątki
    }

    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange("ai.exchange");
    }

    @Bean
    public MessageConverter jackson2MessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // Propagacja kontekstu trace do nagłówków publikowanych wiadomości.
        template.setObservationEnabled(true);

        return template;
    }

    @Bean
    public Queue ollamaQueue() {
        return QueueBuilder.durable("ai." + AiProvider.OLLAMA.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding ollamaBinding(Queue ollamaQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(ollamaQueue)
                .to(aiExchange)
                .with("ai.*.ollama");
    }

    @Bean
    public Queue groqQueue() {
        return QueueBuilder.durable("ai." + AiProvider.GROQ.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding groqBinding(Queue groqQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(groqQueue)
                .to(aiExchange)
                .with("ai.*.groq");
    }

    @Bean
    public Queue mistralQueue() {

        return QueueBuilder.durable("ai." + AiProvider.MISTRAL.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding mistralBinding(Queue mistralQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(mistralQueue)
                .to(aiExchange)
                .with("ai.*.mistral");
    }

    @Bean
    public Queue nvidiaQueue() {
        return QueueBuilder.durable("ai." + AiProvider.NVIDIA.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding nvidiaBinding(Queue nvidiaQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(nvidiaQueue)
                .to(aiExchange)
                .with("ai.*.nvidia");
    }

    @Bean
    public Queue togetherQueue() {
        return QueueBuilder.durable("ai." + AiProvider.TOGETHER.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding togetherBinding(Queue togetherQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(togetherQueue)
                .to(aiExchange)
                .with("ai.*.together");
    }

    @Bean
    public Queue openaiQueue() {
        return QueueBuilder.durable("ai." + AiProvider.OPENAI.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding openAiBinding(Queue openaiQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(openaiQueue)
                .to(aiExchange)
                .with("ai.*.openai");
    }

    @Bean
    public Queue hyperbolicQueue() {
        return QueueBuilder.durable("ai." + AiProvider.HYPERBOLIC.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding hyperbolicBinding(Queue hyperbolicQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(hyperbolicQueue)
                .to(aiExchange)
                .with("ai.*.hyperbolic");
    }

    @Bean
    public Queue anthropicQueue() {
        return QueueBuilder.durable("ai." + AiProvider.ANTHROPIC.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding anthropicBinding(Queue anthropicQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(anthropicQueue)
                .to(aiExchange)
                .with("ai.*.anthropic");
    }

    @Bean
    public Queue geminiQueue() {
        return QueueBuilder.durable("ai." + AiProvider.GEMINI.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding geminiBinding(Queue geminiQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(geminiQueue)
                .to(aiExchange)
                .with("ai.*.gemini");
    }

    @Bean
    public Queue cohereQueue() {
        return QueueBuilder.durable("ai." + AiProvider.COHERE.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding cohereBinding(Queue cohereQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(cohereQueue)
                .to(aiExchange)
                .with("ai.*.cohere");
    }

    @Bean
    public Queue openRouterQueue() {
        return QueueBuilder.durable("ai." + AiProvider.OPENROUTER.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding openRouterBinding(Queue openRouterQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(openRouterQueue)
                .to(aiExchange)
                .with("ai.*.openrouter");
    }

    @Bean
    public Queue cloudFlareQueue() {
        return QueueBuilder.durable("ai." + AiProvider.CLOUDFLARE.name().toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding cloudFlareBinding(Queue cloudFlareQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(cloudFlareQueue)
                .to(aiExchange)
                .with("ai.*.cloudflare");
    }


    @Bean
    public Queue embeddedQueue() {
        return QueueBuilder.durable("ai." + EMBEDDED.toLowerCase(Locale.ROOT) + ".queue")
                .maxPriority(MAX_PRIORITY)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }


    @Bean
    public Binding embeddedBinding(Queue embeddedQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(embeddedQueue)
                .to(aiExchange)
                .with("ai.*.embedded");
    }


    @Bean
    public Queue photoDescribedEventQueue() {
        return QueueBuilder.durable("app.vector.save.queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding photoDescribedEventBinding(Queue photoDescribedEventQueue,
                                              TopicExchange aiExchange) {
        return BindingBuilder
                .bind(photoDescribedEventQueue)
                .to(aiExchange)
                .with("ai.event.photo.described");
    }

    @Bean
    public Queue photoProcessedQueue() {
        return QueueBuilder.durable("app.photo.save.queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding photoProcessedBinding(Queue photoProcessedQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(photoProcessedQueue)
                .to(aiExchange)
                .with("ai.event.photo.processed");
    }

    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder.durable("ai.user.deleted.queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(userDeletedQueue)
                .to(aiExchange)
                .with("app.user.cleanup.queue");
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable("app.audit.queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange aiExchange) {
        return BindingBuilder
                .bind(auditQueue)
                .to(aiExchange)
                .with("app.event.photo.audited");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlq").build();
    }
}
