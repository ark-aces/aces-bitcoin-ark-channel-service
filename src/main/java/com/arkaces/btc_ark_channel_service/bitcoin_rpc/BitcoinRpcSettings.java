package com.arkaces.btc_ark_channel_service.bitcoin_rpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bitcoinRpc")
public class BitcoinRpcSettings {
    private String url;
    private String username;
    private String password;
}
