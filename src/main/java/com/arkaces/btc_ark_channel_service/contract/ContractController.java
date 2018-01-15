package com.arkaces.btc_ark_channel_service.contract;

import com.arkaces.ApiException;
import com.arkaces.aces_listener_api.AcesListenerApi;
import com.arkaces.aces_server.aces_service.contract.Contract;
import com.arkaces.aces_server.aces_service.contract.ContractStatus;
import com.arkaces.aces_server.aces_service.contract.CreateContractRequest;
import com.arkaces.aces_server.aces_service.error.ServiceErrorCodes;
import com.arkaces.aces_server.common.error.NotFoundException;
import com.arkaces.aces_server.common.identifer.IdentifierGenerator;
import io.swagger.client.model.Subscription;
import io.swagger.client.model.SubscriptionRequest;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContractController {
    
    private final IdentifierGenerator identifierGenerator;
    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final AcesListenerApi bitcoinListener;
    private final String bitcoinEventCallbackUrl;
    
    @PostMapping("/contracts")
    public Contract<Results> postContract(@RequestBody CreateContractRequest<Arguments> createContractRequest) {
        ContractEntity contractEntity = new ContractEntity();
        contractEntity.setCorrelationId(createContractRequest.getCorrelationId());
        contractEntity.setCreatedAt(LocalDateTime.now());
        contractEntity.setId(identifierGenerator.generate());
        contractEntity.setStatus(ContractStatus.EXECUTED);
        
        // generate bitcoin wallet for deposits
        ECKey key = new ECKey();
        String depositBtcAddress = Hex.toHexString(key.getPubKeyHash());
        contractEntity.setDepositBtcAddress(depositBtcAddress);
        String depositBtcPrivateKey = Hex.toHexString(key.getPrivKeyBytes());
        contractEntity.setDepositBtcPassphrase(depositBtcPrivateKey);

        // subscribe to bitcoin listener on deposit bitcoin address
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setCallbackUrl(bitcoinEventCallbackUrl);
        subscriptionRequest.setMinConfirmations(2);
        subscriptionRequest.setRecipientAddress(depositBtcAddress);
        Subscription subscription;
        try {
            subscription = bitcoinListener.subscriptionsPost(subscriptionRequest);
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
