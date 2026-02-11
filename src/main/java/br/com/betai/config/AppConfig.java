package br.com.betai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000); // 20 segundos
        factory.setReadTimeout(120000); // 120 segundos (2 minutos) para análise profunda e cálculos EV
        return new RestTemplate(factory);
    }

    @SuppressWarnings("null")
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.builder().configure(options -> options.maxMessagesPerPoll(1) // Pega
                                                                                                               // apenas
                                                                                                               // 1 por
                                                                                                               // vez
                .maxConcurrentMessages(1) // Garante apenas 1 thread ativa
        ).sqsAsyncClient(sqsAsyncClient).build();
    }
}
