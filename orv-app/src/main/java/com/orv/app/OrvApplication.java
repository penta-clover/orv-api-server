package com.orv.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.orv.app",
        "com.orv.common",
        "com.orv.health",
        "com.orv.auth",
        "com.orv.archive",
        "com.orv.storyboard",
        "com.orv.media",
        "com.orv.reservation",
        "com.orv.notification",
        "com.orv.recap",
        "com.orv.term",
        "com.orv.admin"
})
public class OrvApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrvApplication.class, args);
	}
}
