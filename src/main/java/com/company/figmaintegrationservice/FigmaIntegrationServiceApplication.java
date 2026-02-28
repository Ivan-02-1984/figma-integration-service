package com.company.figmaintegrationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.awt.Desktop;
import java.net.URI;

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
            // Современный кроссплатформенный способ (Java 6+)
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🌐 Браузер открыт: " + url);
                return;
            }

            // Fallback для старых систем или специфичных случаев
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("mac")) {
                // macOS
                rt.exec(new String[]{"open", url});
                System.out.println("🌐 Открываем браузер на macOS: " + url);
            } else if (os.contains("win")) {
                // Windows (как у вас сейчас)
                rt.exec(new String[]{"cmd", "/c", "start", url});
                System.out.println("🌐 Открываем браузер на Windows: " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux
                rt.exec(new String[]{"xdg-open", url});
                System.out.println("🌐 Открываем браузер на Linux: " + url);
            } else {
                System.out.println("⚠️ Не удалось определить ОС. Откройте браузер вручную: " + url);
            }

        } catch (Exception e) {
            // Не падаем, если браузер не открылся
            System.out.println("⚠️ Браузер не открылся автоматически. Откройте вручную: " + url);
        }
    }
}