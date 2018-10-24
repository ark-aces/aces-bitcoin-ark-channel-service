package com.arkaces.btc_ark_channel_service.transfer;

import com.arkaces.aces_server.common.identifer.IdentifierGenerator;
import com.arkaces.aces_server.aces_service.notification.NotificationService;
import com.arkaces.btc_ark_channel_service.Constants;
import com.arkaces.btc_ark_channel_service.ark.ArkService;
import com.arkaces.btc_ark_channel_service.bitcoin_rpc.BitcoinService;
import com.arkaces.btc_ark_channel_service.contract.ContractEntity;
import com.arkaces.btc_ark_channel_service.service_capacity.ServiceCapacityEntity;
import com.arkaces.btc_ark_channel_service.service_capacity.ServiceCapacityRepository;
import com.arkaces.btc_ark_channel_service.service_capacity.ServiceCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final ArkService arkService;
    private final BitcoinService bitcoinService;
    private final BigDecimal lowCapacityThreshold;
    private final NotificationService notificationService;
    private final ServiceCapacityService serviceCapacityService;
    private final ServiceCapacityRepository serviceCapacityRepository;

    /**
     * @return true if amount reserved successfully
     */
    public boolean reserveTransferCapacity(Long transferPid) {
        // Lock service capacity and update available balance if available
        ServiceCapacityEntity serviceCapacityEntity = serviceCapacityService.getLockedCapacityEntity();

        TransferEntity transferEntity = transferRepository.findOneForUpdate(transferPid);
        BigDecimal totalAmount = transferEntity.getArkSendAmount().add(Constants.ARK_TRANSACTION_FEE);
        BigDecimal newAvailableAmount = serviceCapacityEntity.getAvailableAmount().subtract(totalAmount);
        BigDecimal newUnsettledAmount = serviceCapacityEntity.getUnsettledAmount().add(totalAmount);
        if (newAvailableAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        serviceCapacityEntity.setAvailableAmount(newAvailableAmount);
        serviceCapacityEntity.setUnsettledAmount(newUnsettledAmount);
        serviceCapacityRepository.save(serviceCapacityEntity);

        if (serviceCapacityEntity.getAvailableAmount().compareTo(lowCapacityThreshold) <= 0) {
            notificationService.notifyLowCapacity(serviceCapacityEntity.getAvailableAmount(), serviceCapacityEntity.getUnit());
        }
        
        return true;
    }
    
    public void settleTransferCapacity(Long transferPid) {
        ServiceCapacityEntity serviceCapacityEntity = serviceCapacityService.getLockedCapacityEntity();

        TransferEntity transferEntity = transferRepository.findOne(transferPid);
        BigDecimal totalAmount = transferEntity.getArkSendAmount().add(Constants.ARK_TRANSACTION_FEE);

        serviceCapacityEntity.setUnsettledAmount(serviceCapacityEntity.getUnsettledAmount().subtract(totalAmount));
        serviceCapacityEntity.setTotalAmount(serviceCapacityEntity.getTotalAmount().subtract(totalAmount));

        serviceCapacityRepository.save(serviceCapacityEntity);

    }
    
    public void processNewTransfer(Long transferPid) {
        TransferEntity transferEntity = transferRepository.findOneForUpdate(transferPid);
        ContractEntity contractEntity = transferEntity.getContractEntity();

        BigDecimal totalAmount = transferEntity.getArkSendAmount().add(Constants.ARK_TRANSACTION_FEE);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal arkSendAmount = transferEntity.getArkSendAmount();
            String recipientArkAddress = contractEntity.getRecipientArkAddress();
            String arkTransactionId = arkService.sendTransaction(recipientArkAddress, arkSendAmount);
            transferEntity.setArkTransactionId(arkTransactionId);

            log.info("Sent " + arkSendAmount + " ark to " + contractEntity.getRecipientArkAddress()
                + ", ark transaction id " + arkTransactionId + ", btc transaction " + transferEntity.getBtcTransactionId());
        } 
        
        transferEntity.setStatus(TransferStatus.COMPLETE);
        transferRepository.save(transferEntity);
        log.info("Saved transfer id " + transferEntity.getId() + " to contract " + contractEntity.getId());
        notificationService.notifySuccessfulTransfer(
                transferEntity.getContractEntity().getId(),
                transferEntity.getId()
        );
    }

    /**
     * Process a full return due to insufficient capacity
     * @param transferPid
     */
    public void processReturn(Long transferPid) {
        TransferEntity transferEntity = transferRepository.findOneForUpdate(transferPid);
        String returnedMessage = "Insufficient ark to send transfer id = " + transferEntity.getId();
        log.info(returnedMessage);

        String returnBtcAddress = transferEntity.getContractEntity().getReturnBtcAddress();
        if (returnBtcAddress != null) {
            String returnBtcTransactionId = bitcoinService.sendTransaction(returnBtcAddress, transferEntity.getBtcAmount());
            transferEntity.setStatus(TransferStatus.RETURNED);
            notificationService.notifyFailedTransfer(
                    transferEntity.getContractEntity().getId(),
                    transferEntity.getId(),
                    returnedMessage
            );
            transferEntity.setReturnBtcTransactionId(returnBtcTransactionId);
        } else {
            String failedMessage = "Bitcoin return could not be processed for transfer " + transferPid;
            log.warn(failedMessage);
            transferEntity.setStatus(TransferStatus.FAILED);
            notificationService.notifyFailedTransfer(
                    transferEntity.getContractEntity().getId(),
                    transferEntity.getId(),
                    failedMessage
            );
        }

        transferRepository.save(transferEntity);
    }
    
    public void processFailedTransfer(Long transferPid) {
        TransferEntity transferEntity = transferRepository.findOneForUpdate(transferPid);
        transferEntity.setStatus(TransferStatus.FAILED);
        notificationService.notifyFailedTransfer(
                transferEntity.getContractEntity().getId(),
                transferEntity.getId(),
                "Transfer failed."
        );
        transferRepository.save(transferEntity);

    }
    
}
