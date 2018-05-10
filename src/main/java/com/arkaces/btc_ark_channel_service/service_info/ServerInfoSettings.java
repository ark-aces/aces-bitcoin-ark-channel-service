package com.arkaces.btc_ark_channel_service.service_info;

import com.arkaces.aces_server.aces_service.server_info.Capacity;
import com.arkaces.aces_server.aces_service.server_info.MoneyAmount;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "serverInfo")
public class ServerInfoSettings {
    private String name;
    private String description;
    private String version;
    private String websiteUrl;
    private String instructions;
    private MoneyAmount flatFee;
    private BigDecimal percentFee;
    private List<Capacity> capacities;
    private String inputSchema;
    private String outputSchema;
    private List<String> interfaces;
}
