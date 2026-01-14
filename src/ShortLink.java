package SLS;

import java.time.LocalDateTime;
import java.util.UUID;

public class ShortLink {
    //Поля
    private String id;
    private String originalUrl;
    private String shortCode;
    private String userId;
    private int clickCount;
    private int clickLimit;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    //Конструктор создания новой ссылки
    public ShortLink(String originalUrl, String userId, int clickLimit, String shortCode) {
        this.id = UUID.randomUUID().toString();
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.clickLimit = clickLimit;
        this.shortCode = shortCode;
        this.clickCount = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusDays(1); // Срок действия 1 день
        this.active = true;
    }

    // Геттеры
    public String getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getShortCode() { return shortCode; }
    public String getUserId() { return userId; }
    public int getClickCount() { return clickCount; }
    public int getClickLimit() { return clickLimit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    //Сеттеры
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    public void setActive(boolean active) { this.active = active; }

    public void incrementClickCount() {
        this.clickCount++;
    }
    //И немного ясности
    @Override
    public String toString() {
        return "ShortLink{" +
                "id='" + id + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", shortCode='" + shortCode + '\'' +
                ", userId='" + userId + '\'' +
                ", clickCount=" + clickCount +
                ", clickLimit=" + clickLimit +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                '}';
    }
}
