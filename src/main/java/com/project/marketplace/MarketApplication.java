package com.project.marketplace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@MapperScan("com.project.marketplace")
public class MarketApplication {
//	MarketuserApplication
	public static void main(String[] args) {
		SpringApplication.run(MarketApplication.class, args);
	}

}
