package com.atleta.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.atleta.demo.ai.AiProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AiProperties.class)
public class ServerAtletaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerAtletaApplication.class, args);
	}

}
