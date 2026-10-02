package eu.mm.software.photofinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableRetry
@EnableAsync
@EnableCaching
public class PhotoFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotoFinderApplication.class, args);
    }

}
