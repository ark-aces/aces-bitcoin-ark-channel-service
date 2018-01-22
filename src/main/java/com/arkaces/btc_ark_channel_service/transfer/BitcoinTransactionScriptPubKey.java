package com.arkaces.btc_ark_channel_service.transfer;

import lombok.Data;

import java.util.List;

@Data
public class BitcoinTransactionScriptPubKey {
    private String type;
    private List<String> addresses;
}
