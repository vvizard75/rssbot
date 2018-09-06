package ru.vvizard.rssspy.botservice.utils;

import org.springframework.data.annotation.Transient;


public enum Lang {
    RU("ru"),
    GB("en"),
    ES("es");

    final String lang;

    @Transient
    Emoji flag;

    Lang(String lang){
        this.lang=lang;

    }



}
