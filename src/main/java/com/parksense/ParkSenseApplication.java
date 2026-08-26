package com.parksense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * ParkSense - Smart Parking Management System.
 *
 * The entire domain model is built from plain Java objects with no framework
 * annotations; Spring only boots the HTTP layer. All object wiring happens in
 * {@link com.parksense.app.AppConfig}, so every design pattern in the system can
 * be read and tested as ordinary Java.
 */
@SpringBootApplication
public class ParkSenseApplication {

    private final Environment env;

    public ParkSenseApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(ParkSenseApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String url = "http://localhost:" + port + contextPath;

        System.out.println();
        System.out.println("  ============================================================");
        System.out.println("  🚀 ParkSense is running successfully!");
        System.out.println("  👉 Live URL:  " + url);
        System.out.println("  👉 Accounts:  admin / admin123  (Admin)");
        System.out.println("                operator / operator123 (Operator)");
        System.out.println("  ============================================================");
        System.out.println();
    }
}

