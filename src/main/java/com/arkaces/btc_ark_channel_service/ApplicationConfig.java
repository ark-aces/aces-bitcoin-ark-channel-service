package com.arkaces.btc_ark_channel_service;

import ark_java_client.*;
import com.arkaces.ApiClient;
import com.arkaces.aces_listener_api.AcesListenerApi;
import com.arkaces.aces_server.aces_service.config.AcesServiceConfig;
import com.arkaces.aces_server.ark_auth.ArkAuthConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Import({AcesServiceConfig.class, ArkAuthConfig.class})
@EnableJpaRepositories
@EntityScan
public class ApplicationConfig {

    @Bean
    public ArkClient arkClient(Environment environment) {
        ArkNetworkFactory arkNetworkFactory = new ArkNetworkFactory();
        String arkNetworkConfigPath = environment.getProperty("arkNetworkConfigPath");
        ArkNetwork arkNetwork = arkNetworkFactory.createFromYml(arkNetworkConfigPath);

        HttpArkClientFactory httpArkClientFactory = new HttpArkClientFactory();
        return httpArkClientFactory.create(arkNetwork);
    }

    @Bean
    public AcesListenerApi bitcoinListener(Environment environment) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(environment.getProperty("bitcoinListener.url"));
        if (environment.containsProperty("bitcoinListener.apikey")) {
            apiClient.setUsername("token");
            apiClient.setPassword(environment.getProperty("bitcoinListener.apikey"));
        }

        return new AcesListenerApi(apiClient);
    }

    @Bean
    public String bitcoinEventCallbackUrl(Environment environment) {
        return environment.getProperty("bitcoinEventCallbackUrl");
    }

}
