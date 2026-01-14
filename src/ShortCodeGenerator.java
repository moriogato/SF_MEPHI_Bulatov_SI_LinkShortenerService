package SLS;

import java.security.SecureRandom; //Генератор случайных чисел

public class ShortCodeGenerator {
    //Составляем алфавит для генерации кода
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    //Задаем длину слова
    private static final int CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    public String generateCode(String originalUrl, String userId) {
        // Создаем уникальную строку на основе URL, userId и времени
        String uniqueString = originalUrl + userId + System.nanoTime();

        // Генерируем случайный код
        StringBuilder sb = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = Math.abs(random.nextInt()) % ALPHABET.length();
            sb.append(ALPHABET.charAt(index));
        }

        return sb.toString();
    }
}
