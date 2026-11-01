package com.docsaas.api;

import javax.imageio.ImageIO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocsaasApiApplication {

	public static void main(String[] args) {
		// Register ImageIO plugins on the classpath (WEBP, JBIG2) before any image work.
		ImageIO.scanForPlugins();
		SpringApplication.run(DocsaasApiApplication.class, args);
	}

}
