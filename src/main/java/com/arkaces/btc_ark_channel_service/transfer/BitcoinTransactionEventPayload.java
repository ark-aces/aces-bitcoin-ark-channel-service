package com.arkaces.btc_ark_channel_service.transfer;

import lombok.Data;

@Data
public class BitcoinTransactionEventPayload {
    private String id;
    private String transactionId;
    private BitcoinTransaction data;
    private String createdAt;
    private String subscriptionId;
}

