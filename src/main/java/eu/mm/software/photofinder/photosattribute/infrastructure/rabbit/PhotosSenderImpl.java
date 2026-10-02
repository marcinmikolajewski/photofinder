package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.PhotoParamsDto;
import eu.mm.software.photofinder.photosattribute.infrastructure.PhotosSender;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
class PhotosSenderImpl implements PhotosSender {

    private final RabbitTemplate rabbitTemplate;
    private final UserQuery userQuery;

    @Override
    @Retryable(
            retryFor = {AmqpException.class, IOException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000)
    )
    public void sendMessage(String queue, PhotoParamsDto photoParamsDto, int priority) {

        String userEmail = userQuery.findUserNameByUserId(photoParamsDto.getUserId());
        log.info("Sending message to queue {}", queue + " file: " + photoParamsDto.getFileName() + " by " + userEmail);

        rabbitTemplate.convertAndSend("ai.exchange",
                "ai.describe." + queue.toLowerCase(Locale.ROOT),
                photoParamsDto, message -> {
                    message.getMessageProperties().setPriority(priority);
                    return message;
                });
    }
}
