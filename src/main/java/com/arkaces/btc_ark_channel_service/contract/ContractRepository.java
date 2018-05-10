package com.arkaces.btc_ark_channel_service.contract;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ContractRepository extends PagingAndSortingRepository<ContractEntity, Long> {
    ContractEntity findOneById(String id);

    ContractEntity findOneBySubscriptionId(String subscriptionId);
    
    ContractEntity findOneByCorrelationId(String correlationId);
}
