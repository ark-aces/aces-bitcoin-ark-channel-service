package com.arkaces.btc_ark_channel_service.contract;

import com.arkaces.ApiException;
import com.arkaces.aces_listener_api.AcesListenerApi;
import com.arkaces.aces_server.aces_service.contract.Contract;
import com.arkaces.aces_server.aces_service.contract.ContractStatus;
import com.arkaces.aces_server.aces_service.contract.CreateContractRequest;
import com.arkaces.aces_server.aces_service.error.ServiceErrorCodes;
import com.arkaces.aces_server.common.error.NotFoundException;
import com.arkaces.aces_server.common.identifer.IdentifierGenerator;
import com.arkaces.btc_ark_channel_service.bitcoin_rpc.BitcoinService;
import io.swagger.client.model.Subscription;
import io.swagger.client.model.SubscriptionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ContractController {
    
    private final IdentifierGenerator identifierGenerator;
    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final AcesListenerApi bitcoinListener;
    private final String bitcoinEventCallbackUrl;
    private final BitcoinService bitcoinService;
    
    @PostMapping("/contracts")
    public Contract<Results> postContract(@RequestBody CreateContractRequest<Arguments> createContractRequest) {
        ContractEntity contractEntity = new ContractEntity();
        contractEntity.setCorrelationId(createContractRequest.getCorrelationId());
        contractEntity.setRecipientArkAddress(createContractRequest.getArguments().getRecipientArkAddress());
        contractEntity.setCreatedAt(LocalDateTime.now());
        contractEntity.setId(identifierGenerator.generate());
        contractEntity.setStatus(ContractStatus.EXECUTED);
        
        // generate bitcoin wallet for deposits
        String depositBtcAddress = bitcoinService.getNewAddress();
        contractEntity.setDepositBtcAddress(depositBtcAddress);

        // subscribe to bitcoin listener on deposit bitcoin address
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setCallbackUrl(bitcoinEventCallbackUrl);
        subscriptionRequest.setMinConfirmations(0);
        subscriptionRequest.setRecipientAddress(depositBtcAddress);
        Subscription subscription;
        try {
            subscription = bitcoinListener.subscriptionsPost(subscriptionRequest);
            log.info("subscription: " + subscription.toString());
        } catch (ApiException e) {
            throw new RuntimeException("Bitcoin Listener subscription failed to POST", e);
        }
        contractEntity.setSubscriptionId(subscription.getId());

        contractRepository.save(contractEntity);

        return contractMapper.map(contractEntity);
    }
    
    @GetMapping("/contracts/{contractId}")
    public Contract<Results> getContract(@PathVariable String contractId) {
        ContractEntity contractEntity = contractRepository.findOneById(contractId);
        if (contractEntity == null) {
            throw new NotFoundException(ServiceErrorCodes.CONTRACT_NOT_FOUND, "Contract not found with id = " + contractId);
        }
        
        return contractMapper.map(contractEntity);
    }
}
