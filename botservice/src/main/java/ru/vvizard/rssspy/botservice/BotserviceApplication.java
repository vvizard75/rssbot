package ru.vvizard.rssspy.botservice;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


@SpringBootApplication
@EnableScheduling
@Configuration

public class BotserviceApplication {
	private static final Logger logger= LogManager.getLogger(BotserviceApplication.class);

	public static final List<String> availableLocales=new ArrayList<>();


	public static void main(String[] args) {
		logger.info("Start application");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(BotserviceApplication.class, args);
	}

	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("i18n/messages");
		messageSource.setDefaultEncoding("UTF-8");



        availableLocales.add("ru");
        availableLocales.add("gb");
        availableLocales.add("es");
		logger.info("End search lang names");
		return messageSource;
	}

	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver slr = new SessionLocaleResolver();
		slr.setDefaultLocale(Locale.ENGLISH); // Set default Locale as US
		return slr;
	}



	@Configuration
	public static class WebappConfig extends WebMvcConfigurerAdapter {
		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
			builder
					.serializerByType(ObjectId.class, new ToStringSerializer());
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(builder.build());
			converters.add(converter);
		}
	}


}
