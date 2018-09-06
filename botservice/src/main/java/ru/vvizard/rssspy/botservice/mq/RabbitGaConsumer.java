package ru.vvizard.rssspy.botservice.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.vvizard.rssspy.botservice.utils.General;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by igormazevich
 */
@Component
public class RabbitGaConsumer {

    private static final Logger logger= LogManager.getLogger(RabbitGaConsumer.class);
    private static final String err_Json="ERROR: to JSON: ";

    @RabbitListener(queues = RabbitConfiguration.QUEUE_GANALYTICS_MSG)
    public void addChatToFeed(String strJson){
        Map<String, String> message= null;

        try {
            message = General.mapper.readValue(strJson, new TypeReference<Map<String, String>>(){});
        } catch (IOException e) {
            logger.error(err_Json+strJson);
            e.printStackTrace();
        }
        if (message==null){
            return;
        }

        javax.net.ssl.SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException |KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
            return;
        }

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);


        String url = "https://www.google-analytics.com/collect";
        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sslsf).setUserAgent("MagpieRSS/0.7 ( http://magpierss.sf.net)").build()) {
            List<NameValuePair> nameValuePairs=new ArrayList<>();
            message.forEach((k, v)-> nameValuePairs.add(new BasicNameValuePair(k, v)));
            nameValuePairs.add(new BasicNameValuePair("v", "1"));
            nameValuePairs.add(new BasicNameValuePair("tid", "UA-32885063-2"));
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            client.execute(httpPost);
        } catch (IOException e) {
            logger.error("ERROR to send GA ");

        }

    }
}

