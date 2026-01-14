package SLS;

//import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
//Планировщик уборки
public class LinkExpirationService {
    //Ссылка на основной сервис - объект уборки
    private final LinkShortenerService linkService;
    //Таймер уборки
    private ScheduledExecutorService scheduler;
    //Ссылка на задачу, чтобы можно было отменить
    private ScheduledFuture<?> cleanupTask;

    public LinkExpirationService(LinkShortenerService linkService) {
        this.linkService = linkService; //Убираем здесь
    }

    public void start() {
        scheduler = Executors.newScheduledThreadPool(1);

        // Запускаем задачу очистки каждые 30 минут
        cleanupTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                linkService.cleanupExpiredLinks(); //вызываем из LinkShortenerService
            } catch (Exception e) {
                System.err.println("Ошибка при очистке ссылок: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.MINUTES);//Начать сразу, повторять раз в полчаса

        System.out.println("Служба очистки ссылок запущена (проверка каждые 30 минут)");
    }

    //Прописываем корректное завершение работы
    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        System.out.println("Служба очистки ссылок остановлена");
    }
}