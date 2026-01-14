package SLS;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;  //Мапа для хранения
import java.util.stream.Collectors;             //Сбор информации из потока в список

public class LinkShortenerService {
    //Объявляем поля данных, название ссылки и информация о ней в хранилище ссылок
    private final Map<String, ShortLink> linksByCode = new ConcurrentHashMap<>();
    //Пользователь и его ссылки
    private final Map<String, List<String>> userLinks = new ConcurrentHashMap<>();
    //Генератор кодов для коротких ссылок
    private final ShortCodeGenerator codeGenerator = new ShortCodeGenerator();

    public ShortLink createShortLink(String originalUrl, String userId, int clickLimit) {
        // Валидация URL
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }

        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            originalUrl = "https://" + originalUrl;
        }

        if (clickLimit <= 0) {
            throw new IllegalArgumentException("Лимит переходов должен быть положительным числом");
        }

        // Генерируем уникальный код
        String shortCode;
        do {
            shortCode = codeGenerator.generateCode(originalUrl, userId);
        } while (linksByCode.containsKey(shortCode));

        // Создаем ссылку
        ShortLink link = new ShortLink(originalUrl, userId, clickLimit, shortCode);

        // Сохраняем
        linksByCode.put(shortCode, link);
        userLinks.computeIfAbsent(userId, k -> new ArrayList<>()).add(shortCode);

        System.out.println("Создана новая ссылка: " + shortCode);
        return link;
    }
    //Ищем ссылку в хранилище по короткому коду
    public Optional<String> processRedirect(String shortCode) {
        ShortLink link = linksByCode.get(shortCode);
        //Проверяем активна ли она
        if (link == null || !link.isActive()) {
            System.out.println("\nСсылка " + shortCode + " неактивна или не найдена");
            return Optional.empty();
        }

        // Проверяем срок действия
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            link.setActive(false);
            System.out.println("Ссылка " + shortCode + " истекла");
            return Optional.empty();
        }

        // Проверяем лимит переходов
        if (link.getClickCount() >= link.getClickLimit()) {
            link.setActive(false);
            System.out.println("Лимит переходов исчерпан для ссылки " + shortCode);
            return Optional.empty();
        }

        // Увеличиваем счетчик переходов
        link.incrementClickCount();

        System.out.println("Перенаправление по ссылке " + shortCode +
                " (переходов: " + link.getClickCount() + "/" + link.getClickLimit() + ")");
        //Возвращаем URL
        return Optional.of(link.getOriginalUrl());
    }
    //Находим список кодов ссылок пользователя
    public List<ShortLink> getUserLinks(String userId) {
        List<String> linkCodes = userLinks.get(userId);
        //Если пользователя нет, то пустой список
        if (linkCodes == null) {
            return Collections.emptyList();
        }
        //Преобразуем коды в объекты ShortLink и собираем в список
        return linkCodes.stream()
                .map(linksByCode::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    //Удаление ссылки
    public boolean deleteLink(String userId, String shortCode) {
        //Находим ссылку
        ShortLink link = linksByCode.get(shortCode);
        //Проверяем ее
        if (link == null || !link.getUserId().equals(userId)) {
            return false;
        }

        //И удаляем из мапы
        linksByCode.remove(shortCode);
        //Вычищаем из списка пользователя
        List<String> userLinkCodes = userLinks.get(userId);
        if (userLinkCodes != null) {
            userLinkCodes.remove(shortCode);
            if (userLinkCodes.isEmpty()) {
                userLinks.remove(userId);
            }
        }

        System.out.println("Ссылка " + shortCode + " удалена пользователем " + userId);
        return true;
    }
    //Удаление просроченных ссылок
    public void cleanupExpiredLinks() {
        LocalDateTime now = LocalDateTime.now();
        //Деактивируем свежие истекшие ссылки
        List<String> expiredCodes = new ArrayList<>();

        for (Map.Entry<String, ShortLink> entry : linksByCode.entrySet()) {
            ShortLink link = entry.getValue();
            if (link.getExpiresAt().isBefore(now) && link.isActive()) {
                link.setActive(false);
                expiredCodes.add(entry.getKey());
                System.out.println("Ссылка " + entry.getKey() + " отмечена как просроченная");
            }
        }

        // Удаляем старые неактивные ссылки (старше 7 дней)
        LocalDateTime weekAgo = now.minusDays(7);
        int removedCount = 0;

        Iterator<Map.Entry<String, ShortLink>> iterator = linksByCode.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ShortLink> entry = iterator.next();
            ShortLink link = entry.getValue();

            if (!link.isActive() && link.getExpiresAt().isBefore(weekAgo)) {
                // Удаляем из userLinks
                List<String> userLinkCodes = userLinks.get(link.getUserId());
                if (userLinkCodes != null) {
                    userLinkCodes.remove(entry.getKey());
                    if (userLinkCodes.isEmpty()) {
                        userLinks.remove(link.getUserId());
                    }
                }

                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            System.out.println("Очищено " + removedCount + " старых неактивных ссылок");
        }
    }

    // Для тестирования можно получить все ссылки
    public Map<String, ShortLink> getAllLinks() {
        return Collections.unmodifiableMap(linksByCode);
    }
}