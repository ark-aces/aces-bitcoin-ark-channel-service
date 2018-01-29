package com.arkaces.btc_ark_channel_service.transfer;

import ark_java_client.ArkClient;
import com.arkaces.aces_server.aces_service.contract.ContractStatus;
import com.arkaces.aces_server.common.identifer.IdentifierGenerator;
import com.arkaces.btc_ark_channel_service.Constants;
import com.arkaces.btc_ark_channel_service.FeeSettings;
import com.arkaces.btc_ark_channel_service.ServiceArkAccountSettings;
import com.arkaces.btc_ark_channel_service.ark.ArkSatoshiService;
import com.arkaces.btc_ark_channel_service.bitcoin_rpc.BitcoinService;
import com.arkaces.btc_ark_channel_service.contract.ContractEntity;
import com.arkaces.btc_ark_channel_service.contract.ContractRepository;
import com.arkaces.btc_ark_channel_service.exchange_rate.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BitcoinTransactionEventHandler {

    private final ContractRepository contractRepository;
    private final TransferRepository transferRepository;
    private final IdentifierGenerator identifierGenerator;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;
    private final ArkSatoshiService arkSatoshiService;
    private final ServiceArkAccountSettings serviceArkAccountSettings;
    private final FeeSettings feeSettings;
    private final BitcoinService bitcoinService;

    @PostMapping("/bitcoinEvents")
    public ResponseEntity<Void> handleBitcoinEvent(@RequestBody BitcoinTransactionEventPayload eventPayload) {
        // todo: verify event post is signed by listener
        String btcTransactionId = eventPayload.getTransactionId();
        
        log.info("Received Bitcoin event: " + btcTransactionId + " -> " + eventPayload.getData());
        
        String subscriptionId = eventPayload.getSubscriptionId();
        ContractEntity contractEntity = contractRepository.findOneBySubscriptionId(subscriptionId);
        if (contractEntity != null) {
            // todo: lock contract for update to prevent concurrent processing of a listener transaction.
            // Listeners send events serially, so that shouldn't be an issue, but we might want to lock
            // to be safe.
            log.info("Matched event for contract id " + contractEntity.getId() + " btc transaction id " + btcTransactionId);

            TransferEntity transferEntity;
            TransferEntity existingTransferEntity = transferRepository.findOneByBtcTransactionId(btcTransactionId);
            if (existingTransferEntity != null) {
                transferEntity = existingTransferEntity;
            } else {
                String transferId = identifierGenerator.generate();

                transferEntity = new TransferEntity();
                transferEntity.setId(transferId);
                transferEntity.setStatus(TransferStatus.NEW);
                transferEntity.setCreatedAt(LocalDateTime.now());
                transferEntity.setBtcTransactionId(btcTransactionId);
                transferEntity.setContractEntity(contractEntity);
            }

            if (! transferEntity.getStatus().equals(TransferStatus.NEW)) {
                log.info("Transfer id " + transferEntity.getId() + " already processed");
                return ResponseEntity.ok().build();
            }

            // Get BTC amount from transaction
            BitcoinTransaction bitcoinTransaction = eventPayload.getData();

            BigDecimal incomingBtcAmount = BigDecimal.ZERO;
            for (BitcoinTransactionVout vout : bitcoinTransaction.getVout()) {
                for (String address : vout.getScriptPubKey().getAddresses()) {
                    if (address.equals(contractEntity.getDepositBtcAddress())) {
                        incomingBtcAmount = incomingBtcAmount.add(vout.getValue());
                    }
                }
            }
            transferEntity.setBtcAmount(incomingBtcAmount);

            BigDecimal btcToArkRate = exchangeRateService.getRate("BTC", "ARK"); //2027.58, Ark 8, Btc 15000
            transferEntity.setBtcToArkRate(btcToArkRate);

            // Deduct fees
            transferEntity.setBtcFlatFee(feeSettings.getBtcFlatFee());
            transferEntity.setBtcPercentFee(feeSettings.getBtcPercentFee());

            BigDecimal percentFee = feeSettings.getBtcPercentFee()
                    .divide(new BigDecimal("100.00"), 8, BigDecimal.ROUND_HALF_UP);
            BigDecimal btcTotalFeeAmount = incomingBtcAmount.multiply(percentFee).add(feeSettings.getBtcFlatFee());
            transferEntity.setBtcTotalFee(btcTotalFeeAmount);

            // Calculate send ark amount
            BigDecimal btcSendAmount = incomingBtcAmount.subtract(btcTotalFeeAmount);
            BigDecimal arkSendAmount = btcSendAmount.multiply(btcToArkRate).setScale(8, RoundingMode.HALF_DOWN);
            if (arkSendAmount.compareTo(Constants.ARK_TRANSACTION_FEE) <= 0) {
                arkSendAmount = BigDecimal.ZERO;
            }
            transferEntity.setArkSendAmount(arkSendAmount);

            transferRepository.save(transferEntity);

            // Check that service has enough ark to send
            SimpleRetryPolicy policy = new SimpleRetryPolicy(5, Collections.singletonMap(Exception.class, true));
            RetryTemplate template = new RetryTemplate();
            template.setRetryPolicy(policy);
            BigDecimal serviceAvailableArk;
            try {
                serviceAvailableArk = template.execute((RetryCallback<BigDecimal, Exception>) context ->
                    arkSatoshiService.toArk(Long.parseLong(
                        arkClient.getBalance(serviceArkAccountSettings.getAddress())
                            .getBalance()))
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse value", e);
            }

            // Send ark transaction
            if (arkSendAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (arkSendAmount.add(Constants.ARK_TRANSACTION_FEE).compareTo(serviceAvailableArk) <= 0) {
                    Long arkSendSatoshis = arkSatoshiService.toSatoshi(arkSendAmount);
                    String arkTransactionId = arkClient.broadcastTransaction(
                            contractEntity.getRecipientArkAddress(),
                            arkSendSatoshis,
                            null,
                            serviceArkAccountSettings.getPassphrase(),
                            10
                    );
                    transferEntity.setArkTransactionId(arkTransactionId);

                    log.info("Sent " + arkSendAmount + " ark to " + contractEntity.getRecipientArkAddress()
                            + ", ark transaction id " + arkTransactionId + ", btc transaction " + btcTransactionId);

                    // todo: asynchronously confirm transaction, if transaction fails to confirm we should return btc amount
                    transferEntity.setNeedsArkConfirmation(true);
                } else {
                    log.info("Insufficient ark to send transfer id = " + transferEntity.getId());
                    // Insufficient service ark to send, we need to return the btc
                    transferEntity.setStatus(TransferStatus.FAILED);

                    // todo: we should automatically send return btc transaction in an async worker
                    transferEntity.setNeedsBtcReturn(true);
                }
            } else {
                transferEntity.setStatus(TransferStatus.COMPLETE);
            }

            transferEntity.setStatus(TransferStatus.COMPLETE);
            transferRepository.save(transferEntity);
            
            log.info("Saved transfer id " + transferEntity.getId() + " to contract " + contractEntity.getId());
        }
        
        return ResponseEntity.ok().build();
    }
}
