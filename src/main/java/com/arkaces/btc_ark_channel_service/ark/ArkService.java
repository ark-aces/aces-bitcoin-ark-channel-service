package com.arkaces.btc_ark_channel_service.ark;

import ark_java_client.ArkClient;
import com.arkaces.btc_ark_channel_service.ServiceArkAccountSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ArkService {
    
    private final ArkClient arkClient;
    private final ServiceArkAccountSettings serviceArkAccountSettings;
    private final ArkSatoshiService arkSatoshiService;
    
    public BigDecimal getServiceArkBalance() {
        return arkSatoshiService.toArk(Long.parseLong(
            arkClient.getBalance(serviceArkAccountSettings.getAddress())
                .getBalance()));
    }
    
    public String sendTransaction(String recipientArkAddress, BigDecimal amount) {
        Long arktoshiAmount = arkSatoshiService.toSatoshi(amount);
        return arkClient.broadcastTransaction(
            recipientArkAddress,
            arktoshiAmount,
            null,
            serviceArkAccountSettings.getPassphrase(),
            10
        );
    }
}
