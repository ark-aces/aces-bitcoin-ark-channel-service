package com.arkaces.btc_ark_channel_service.contract;

import com.arkaces.btc_ark_channel_service.transfer.Transfer;
import lombok.Data;

import java.util.List;

@Data
public class Results {
    private String recipientArkAddress;
    private String returnBtcAddress;
    private String depositBtcAddress;
    private List<Transfer> transfers;
}