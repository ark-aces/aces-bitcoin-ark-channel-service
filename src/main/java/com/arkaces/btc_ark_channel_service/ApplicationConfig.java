package com.arkaces.btc_ark_channel_service;

import ark_java_client.*;
import com.arkaces.ApiClient;
import com.arkaces.aces_listener_api.AcesListenerApi;
import com.arkaces.aces_server.aces_service.config.AcesServiceConfig;
import com.arkaces.aces_server.ark_auth.ArkAuthConfig;
import com.arkaces.btc_ark_channel_service.bitcoin_rpc.BitcoinRpcSettings;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

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
    public RestTemplate bitcoinRpcRestTemplate(BitcoinRpcSettings bitcoinRpcSettings) {
        return new RestTemplateBuilder()
                .rootUri(bitcoinRpcSettings.getUrl())
                .basicAuthorization(bitcoinRpcSettings.getUsername(), bitcoinRpcSettings.getPassword())
                .build();
    }

    @Bean
    public String bitcoinEventCallbackUrl(Environment environment) {
        return environment.getProperty("bitcoinEventCallbackUrl");
    }
    
    @Bean
    public Integer bitcoinListenerMinConfirmations(Environment environment) {
        return environment.getProperty("bitcoinListener.minConfirmations", Integer.class);
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        
        return eventMulticaster;
    }
}
