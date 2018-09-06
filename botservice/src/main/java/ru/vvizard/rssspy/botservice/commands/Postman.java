package ru.vvizard.rssspy.botservice.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramWebhookBot;

/**
 * Created by igormazevich
 * Сервис отправки сообщений
 */
@Component
public class Postman extends TelegramWebhookBot{

    private static final String LOGTAG = "POSTMAN";

    @Value("${FEED_TOKEN}")
    private String token;

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        return null;
    }

    @Override
    public String getBotUsername() {
        return null;
    }

    @Override
    public String getBotPath() {
        return null;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }


}
