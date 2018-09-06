package ru.vvizard.rssspy.botservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.vvizard.rssspy.botservice.utils.Lang;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by igormazevich
 */
@Document(collection="chats")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatVV implements Serializable{

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 7755306600380658861L;

    @Id
    private Long id;

    @SuppressWarnings("unused")
    @Version
    private Long version;

    private String firstName;

    private String lastName;
    private String title;
    private String userName;
    private TypeChat typeChat;


    private ArrayList<Integer> lastNumberMessages =new ArrayList<>();

    @DBRef(lazy = true)
    private HashSet<Feed> feeds=new HashSet<>();

    private Lang lang;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public HashSet<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(HashSet<Feed> feeds) {
        this.feeds = feeds;
    }

    public void addFeed(Feed feed){
        if (this.feeds ==null) {
            this.feeds= new HashSet<>();
        }
        this.feeds.add(feed);
    }

    public ArrayList<Integer> getLastNumberMessages() {
        return lastNumberMessages;
    }

    public void setLastNumberMessages(ArrayList<Integer> lastNumberMessages) {
        this.lastNumberMessages = lastNumberMessages;
    }

    public Boolean pushLastNumberMessage(Integer lastNumberMessage){
       if (this.lastNumberMessages.contains(lastNumberMessage)){
           return false;
       }
        this.lastNumberMessages.add(lastNumberMessage);
        if (this.lastNumberMessages.size()==6){
            this.lastNumberMessages.remove(this.lastNumberMessages.get(0));
        }
        return true;

    }

    public String getLang() {
        return (lang!=null) ? lang.toString() : "gb";
//        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TypeChat getTypeChat() {
        return typeChat;
    }

    public void setTypeChat(TypeChat typeChat) {
        this.typeChat = typeChat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatVV)) return false;

        ChatVV chatVV = (ChatVV) o;

        return getId().equals(chatVV.getId());

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
