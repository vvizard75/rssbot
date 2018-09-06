package ru.vvizard.rssspy.botservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created by igormazevich
 */
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feed implements Serializable{

    private static final long serialVersionUID = 2841153152134760215L;

    @Id
    @JsonDeserialize(using = StringDeserializer.class)
    private ObjectId id;

    @Version
    private Long version;

    private URL url;

    private LocalDateTime dateUpdate;

    private String title;

    private String imageUrl;

    public Boolean getError() {
        return error!=null?error:false;
    }

    public void setError() {
        this.error = true;
    }

    private Boolean error;

    private Integer errCount=0;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @DBRef(lazy = true)
    private HashSet<ChatVV> chats = new HashSet<>();

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public Integer getErrCount() {
        return errCount;
    }

    public void setErrCount(Integer errCount) {
        this.errCount = errCount;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public HashSet<ChatVV> getChats() {
        return chats;
    }

    public void setChats(HashSet<ChatVV> chats) {
        this.chats = chats;
    }

    public void addChat(ChatVV chat){
        if (this.chats ==null) {
            this.chats= new HashSet<>();
        }
        this.chats.add(chat);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Feed)) return false;

        Feed feed = (Feed) o;

        return getUrl().equals(feed.getUrl());

    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

    public LocalDateTime getDateUpdate() {
        return dateUpdate;
    }

    public void setDateUpdate(LocalDateTime dateUpdate) {
        this.dateUpdate = dateUpdate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Feed() {
        this.error=false;
    }
}
