package com.reviewbot.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${github.api.base-url}")
    private String githubBaseUrl;

    @Value("${ai.claude.base-url}")
    private String claudeBaseUrl;

    @Value("${ai.claude.api-key}")
    private String claudeApiKey;

    @Value("${ai.claude.version}")
    private String claudeVersion;

    @Bean
    @Qualifier("githubWebClient")
    public WebClient githubWebClient() {
        return WebClient.builder()
                .baseUrl(githubBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(defaultHttpClient()))
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    @Qualifier("claudeWebClient")
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(claudeBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(defaultHttpClient()))
                .defaultHeader("x-api-key", claudeApiKey)
                .defaultHeader("anthropic-version", claudeVersion)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private HttpClient defaultHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
    }
}
