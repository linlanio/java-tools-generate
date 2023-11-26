package io.linlan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("io.linlan.**.dao")
public class LinlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinlanApplication.class, args);
    }
}
