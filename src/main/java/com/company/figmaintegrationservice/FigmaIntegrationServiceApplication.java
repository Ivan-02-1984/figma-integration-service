package com.company.figmaintegrationservice;

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

        String url = "http://localhost:8095/form.html";

        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();

            if (os.contains("win")) {
                runtime.exec(new String[]{"cmd", "/c", "start", "", url});
            } else if (os.contains("mac")) {
                runtime.exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                runtime.exec(new String[]{"xdg-open", url});
            } else {
                System.out.println("ОС не определена. Откройте вручную: " + url);
                return;
            }

            System.out.println("🌐 Браузер открыт: " + url);

        } catch (Exception e) {
            System.out.println("⚠️ Не удалось открыть браузер автоматически. Откройте вручную: " + url);
        }
    }
}