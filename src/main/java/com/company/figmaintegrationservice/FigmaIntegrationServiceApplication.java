package com.company.figmaintegrationservice;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cache.annotation.EnableCaching;
//
//import java.awt.*;
//import java.net.URI;
//
//@SpringBootApplication
//@EnableCaching
//public class FigmaIntegrationServiceApplication {
//
//    public static void main(String[] args) {
//        SpringApplication.run(FigmaIntegrationServiceApplication.class, args);
//        try {
//            String url = "http://localhost:8095/form.html";
//            if (Desktop.isDesktopSupported()) {
//                Desktop.getDesktop().browse(new URI(url));
//            } else {
//                System.out.println("Откройте браузер: " + url);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication
@EnableCaching
public class FigmaIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FigmaIntegrationServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        try {
            Runtime.getRuntime().exec(
                    "cmd /c start http://localhost:8095/form.html"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}