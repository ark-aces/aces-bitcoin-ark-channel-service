package com.arkaces.btc_ark_channel_service.transfer;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;

@Transactional
public interface TransferRepository extends PagingAndSortingRepository<TransferEntity, Long> {

    TransferEntity findOneByBtcTransactionId(String btcTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct t from TransferEntity t where t.pid = :pid")
    TransferEntity findOneForUpdate(@Param("pid") Long pid);
}
