package com.arkaces.btc_ark_channel_service.transfer;

import lombok.Data;

@Data
public class Transfer {
    private String id;
    private String status;
    private String createdAt;
    private String btcTransactionId;
    private String btcAmount;
    private String btcToArkRate;
    private String btcFlatFee;
    private String btcPercentFee;
    private String btcTotalFee;
    private String arkSendAmount;
    private String arkTransactionId;
    private String returnBtcTransactionId;
}
