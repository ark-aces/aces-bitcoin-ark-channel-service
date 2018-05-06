package com.arkaces.btc_ark_channel_service.contract;

import lombok.Data;

@Data
public class Arguments {
    private String recipientArkAddress;
    private String returnBtcAddress;
}
