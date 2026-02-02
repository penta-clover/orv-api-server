package com.orv.worker.thumbnailextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.orv.worker.thumbnailextraction",
        "com.orv.archive",
        "com.orv.common"
})
public class ThumbnailExtractionWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThumbnailExtractionWorkerApplication.class, args);
    }
}
