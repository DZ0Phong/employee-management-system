package com.group5.ems.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartupLogger {

    private final Environment env;

    @EventListener(ApplicationReadyEvent.class)
    public void multipartDebug() {
        System.out.println("=== MULTIPART CONFIG ===");
        System.out.println("max-file-size: " + env.getProperty("spring.servlet.multipart.max-file-size"));
        System.out.println("max-request-size: " + env.getProperty("spring.servlet.multipart.max-request-size"));
        System.out.println("enabled: " + env.getProperty("spring.servlet.multipart.enabled"));
        System.out.println("========================");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupURL(){
        System.out.println("""
                Server run at: http://localhost:""" + env.getProperty("server.port","8080") + """
                """);
    }
}