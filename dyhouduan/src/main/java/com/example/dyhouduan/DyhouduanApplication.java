package com.example.dyhouduan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.dyhouduan.mapper")
public class DyhouduanApplication {

	public static void main(String[] args) {
		SpringApplication.run(DyhouduanApplication.class, args);
	}

}
