package com.atleta.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerAtletaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerAtletaApplication.class, args);
	}

}
