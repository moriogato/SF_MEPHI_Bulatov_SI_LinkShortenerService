package SLS;

import java.io.*;                  //Работа с вводом-выводом?? Вроде и не понадобился
import java.net.*;                 //Работа с сетью, создание URI
import java.time.LocalDateTime;    //Получение времени
import java.util.*;                //Утилиты работы с данными, не будем углубляться в классы в учебном проекте
import java.util.concurrent.*;     //Реализация многопоточности, например фоновое удаление ссылок
import java.awt.Desktop;           //Открытие ссылок в браузере

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Сервис сокращения ссылок ==="); //Приветственная строка

        try {                                                   //Через трай-кэтч как-то удобнее управляться с этим всем
            LinkShortenerService service = new LinkShortenerService();
            LinkExpirationService expirationService = new LinkExpirationService(service);

            // Запуск службы очистки просроченных ссылок
            expirationService.start();
            //Ждем реакцию на предлагаемое меню
            Scanner scanner = new Scanner(System.in);
            //Собственно меню
            while (true) {
                System.out.println("\nМеню:");
                System.out.println("Создать короткую ссылку - нажмите 1");
                System.out.println("Перейти по короткой ссылке - нажмите 2");
                System.out.println("Показать мои ссылки - нажмите 3");
                System.out.println("Удалить ссылку - нажмите 4");
                System.out.println("Выход - нажмите 5");
                System.out.print("Выберите действие: ");
                //Читаем выбор с проверкой, что число написано правильно
                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Неверный ввод!");
                    continue;
                }
                //В зависимости от выбора пользователя выполняем соответствующий метод
                switch (choice) {
                    case 1:
                        createShortLink(scanner, service);
                        break;
                    case 2:
                        redirectToLink(scanner, service);
                        break;
                    case 3:
                        showUserLinks(scanner, service);
                        break;
                    case 4:
                        deleteLink(scanner, service);
                        break;
                    case 5:
                        System.out.println("\nВыход из программы..."); //В пятом случае все просто, сразу прописано здесь
                        expirationService.stop();
                        scanner.close();
                        return;
                    default:
                        System.out.println("Неверный выбор!"); //Если число не 1-5

                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //Метод если выбор 1
    private static void createShortLink(Scanner scanner, LinkShortenerService service) {
        System.out.print("\nВведите длинный URL: ");
        String originalUrl = scanner.nextLine();

        System.out.print("Введите лимит переходов (по умолчанию 100): ");
        String limitInput = scanner.nextLine();
        int clickLimit = limitInput.isEmpty() ? 100 : Integer.parseInt(limitInput);

        // Получаем или создаем userId
        System.out.print("Введите ваш userId (или оставьте пустым для создания нового): ");
        String userIdInput = scanner.nextLine();
        String userId;

        if (userIdInput.isEmpty()) {
            userId = UUID.randomUUID().toString();
            System.out.println("Ваш новый userId: " + userId);
            System.out.println("Сохраните его для управления ссылками!");
        } else {
            userId = userIdInput;
        }

        try {
            ShortLink link = service.createShortLink(originalUrl, userId, clickLimit);
            System.out.println("\n Ссылка создана успешно!");
            System.out.println("Короткая ссылка: " + link.getShortCode());
            System.out.println("Лимит переходов: " + link.getClickLimit());
            System.out.println("Действительна до: " + link.getExpiresAt());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
    //Метод если выбор 2
    private static void redirectToLink(Scanner scanner, LinkShortenerService service) {
        System.out.print("\nВведите короткий код ссылки: ");
        String shortCode = scanner.nextLine();

        Optional<String> result = service.processRedirect(shortCode);

        if (result.isPresent()) {
            System.out.println("Перенаправление на: " + result.get());

            try {
                // Открываем в браузере
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(result.get()));
                    System.out.println("Ссылка открыта в браузере!");
                } else {
                    System.out.println("Не удалось открыть браузер автоматически.");
                    System.out.println("Скопируйте ссылку вручную: " + result.get());
                }
            } catch (Exception e) {
                System.out.println("Ошибка при открытии браузера: " + e.getMessage());
            }
        } else {
            System.out.println("Ссылка не найдена, истекла или достигнут лимит переходов.");
        }
    }
    //Метод если выбор 3
    private static void showUserLinks(Scanner scanner, LinkShortenerService service) {
        System.out.print("\nВведите ваш userId: ");
        String userId = scanner.nextLine();

        List<ShortLink> links = service.getUserLinks(userId);

        if (links.isEmpty()) {
            System.out.println("У вас нет созданных ссылок.");
        } else {
            System.out.println("\nВаши ссылки:");
            for (ShortLink link : links) {
                System.out.println("\nКод: " + link.getShortCode());
                System.out.println("Оригинал: " + link.getOriginalUrl());
                System.out.println("Короткая ссылка: http://localhost:8080/" + link.getShortCode());
                System.out.println("Переходов: " + link.getClickCount() + "/" + link.getClickLimit());
                System.out.println("Создана: " + link.getCreatedAt());
                System.out.println("Действует до: " + link.getExpiresAt());
                System.out.println("Статус: " + (link.isActive() ? "Активна" : "Неактивна"));
                // Показываем уведомления о проблемах
                if (!link.isActive()) {
                    if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
                        System.out.println("\nСсылка истекла!");
                    } else if (link.getClickCount() >= link.getClickLimit()) {
                        System.out.println("\nЛимит переходов исчерпан!");
                    }
                }
            }
        }
    }
    //Метод если выбор 4
    private static void deleteLink(Scanner scanner, LinkShortenerService service) {
        System.out.print("\nВведите ваш userId: ");
        String userId = scanner.nextLine();

        System.out.print("Введите код ссылки для удаления: ");
        String shortCode = scanner.nextLine();

        boolean deleted = service.deleteLink(userId, shortCode);

        if (deleted) {
            System.out.println("Ссылка "  + shortCode + " удалена успешно!");
        } else {
            System.out.println("Не удалось удалить ссылку. Проверьте userId и код ссылки.");
        }
    }
}
