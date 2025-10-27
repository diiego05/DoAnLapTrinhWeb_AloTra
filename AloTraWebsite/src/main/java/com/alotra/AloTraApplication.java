package com.alotra;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching  // 🆕 Bật cache cho geocoding

public class AloTraApplication {

	public static void main(String[] args) {
		SpringApplication.run(AloTraApplication.class, args);
	}
}