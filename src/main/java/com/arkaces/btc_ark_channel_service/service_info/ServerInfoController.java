package com.arkaces.btc_ark_channel_service.service_info;

import com.arkaces.aces_server.aces_service.server_info.Capacity;
import com.arkaces.aces_server.aces_service.server_info.MoneyAmount;
import com.arkaces.btc_ark_channel_service.FeeSettings;
import com.arkaces.btc_ark_channel_service.service_capacity.ServiceCapacityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ServerInfoController {
    
    private final ServerInfoSettings serverInfoSettings;    
    private final ObjectMapper objectMapper;
    private final ServiceCapacityService serviceCapacityService;
    private final FeeSettings feeSettings;
    
    @GetMapping("/")
    public ServerInfo getServerInfo() {
        JsonNode inputSchemaJsonNode;
        try {
            inputSchemaJsonNode = objectMapper.readTree(serverInfoSettings.getInputSchema());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse inputSchema json", e);
        }

        JsonNode outputSchemaJsonNode;
        try {
            outputSchemaJsonNode = objectMapper.readTree(serverInfoSettings.getOutputSchema());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse outputSchema json", e);
        }

        MoneyAmount flatFee = new MoneyAmount();
        flatFee.setAmount(feeSettings.getBtcFlatFee());
        flatFee.setUnit("BTC");
        
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName(serverInfoSettings.getName());
        serverInfo.setDescription(serverInfoSettings.getDescription());
        serverInfo.setInstructions(serverInfoSettings.getInstructions());
        serverInfo.setVersion(serverInfoSettings.getVersion());
        serverInfo.setWebsiteUrl(serverInfoSettings.getWebsiteUrl());
        serverInfo.setCapacities(serverInfoSettings.getCapacities());
        serverInfo.setFlatFee(flatFee);
        serverInfo.setPercentFee(feeSettings.getBtcPercentFee());
        serverInfo.setInputSchema(inputSchemaJsonNode);
        serverInfo.setOutputSchema(outputSchemaJsonNode);
        serverInfo.setInterfaces(serverInfoSettings.getInterfaces());
        
        if (serverInfoSettings.getCapacities() != null) {
            // Show capacity from config file
            serverInfo.setCapacities(serverInfoSettings.getCapacities());
        } else {
            // Get capacity dynamically from database
            Capacity capacity = new Capacity();
            capacity.setUnit("ARK");
            capacity.setValue(serviceCapacityService.getAvailableAmount());
            serverInfo.setCapacities(Arrays.asList(capacity));
        }
    
        return serverInfo;
    }
}
