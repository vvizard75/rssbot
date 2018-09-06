package ru.vvizard.rssspy.botservice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;


public class General {
    public final static ObjectMapper mapper = new ObjectMapper();
    public final static RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(30000)
            .setConnectTimeout(30000)
            .setConnectionRequestTimeout(30000)
            .build();
}
