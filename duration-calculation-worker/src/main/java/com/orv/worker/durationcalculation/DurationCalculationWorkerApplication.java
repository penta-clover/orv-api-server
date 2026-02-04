package com.orv.worker.durationcalculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.orv.worker.durationcalculation",
        "com.orv.archive",
        "com.orv.common"
})
public class DurationCalculationWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DurationCalculationWorkerApplication.class, args);
    }
}
