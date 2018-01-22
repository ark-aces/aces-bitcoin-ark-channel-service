package com.arkaces.btc_ark_channel_service.transfer;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BitcoinTransactionVout {
    private BigDecimal value;
    private Integer vout;
    private BitcoinTransactionScriptPubKey scriptPubKey;
}
