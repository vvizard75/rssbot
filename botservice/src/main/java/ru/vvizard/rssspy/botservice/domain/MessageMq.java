package ru.vvizard.rssspy.botservice.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by igormazevich
 */
public class MessageMq implements Serializable{
    private Long chatId;
    private String chatName;
    private String title;
    private String url;
    private String description;
    private Date date;
    private String podcast;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPodcast(String podcast) {
        this.podcast = podcast;
    }

    public String getPodcast() {
        return podcast;
    }
}
