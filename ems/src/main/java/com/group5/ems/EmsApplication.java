package com.group5.ems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmsApplication.class, args);
        printStartupInfo();
    }
    public static void printStartupInfo(){
        System.out.println("""
                Server run at: http://localhost:8080
                """);
    }

}

