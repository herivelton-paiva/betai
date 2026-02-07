package br.com.betai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BetaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BetaiApplication.class, args);
	}

}
