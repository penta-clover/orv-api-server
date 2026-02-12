package com.orv.worker.audioextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.orv.worker.audioextraction",
        "com.orv.media",
        "com.orv.archive",
        "com.orv.recap.repository",
        "com.orv.common"
})
public class AudioExtractionWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudioExtractionWorkerApplication.class, args);
    }
}
