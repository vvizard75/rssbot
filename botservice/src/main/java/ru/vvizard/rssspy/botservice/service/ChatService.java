package ru.vvizard.rssspy.botservice.service;

import org.telegram.telegrambots.api.objects.Chat;
import ru.vvizard.rssspy.botservice.domain.ChatVV;

/**
 * Created by igormazevich
 */
public interface ChatService {
    ChatVV create(Chat chat);
    ChatVV findOne(Long id);
    void save(ChatVV chatVV);
    void delete(ChatVV chatVV);
}
