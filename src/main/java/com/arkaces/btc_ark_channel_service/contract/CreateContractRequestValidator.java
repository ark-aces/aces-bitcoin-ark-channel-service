package com.arkaces.btc_ark_channel_service.contract;

import com.arkaces.aces_server.aces_service.contract.CreateContractRequest;
import com.arkaces.aces_server.common.error.ValidatorException;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CreateContractRequestValidator {
    
    private final ContractRepository contractRepository;

    public void validate(CreateContractRequest<Arguments> createContractRequest) {
        BindingResult bindingResult = new BeanPropertyBindingResult(createContractRequest, "createContractRequest");
        
        String correlationId = createContractRequest.getCorrelationId();
        if (! StringUtils.isEmpty(correlationId)) {
            ContractEntity contractEntity = contractRepository.findOneByCorrelationId(correlationId);
            if (contractEntity != null) {
                bindingResult.rejectValue("correlationId", FieldErrorCodes.DUPLICATE_CORRELATION_ID, 
                    "A contract with the given correlation ID already exists.");
            }
        }
        
        String recipientArkAddress = createContractRequest.getArguments().getRecipientArkAddress();
        if (StringUtils.isEmpty(recipientArkAddress)) {
            bindingResult.rejectValue("recipientArkAddress", FieldErrorCodes.REQUIRED, "Recipient ARK address required.");
        } else {
            try {
                Base58.decodeChecked(recipientArkAddress);
            } catch (AddressFormatException exception) {
                if (exception.getMessage().equals("Checksum does not validate")) {
                    bindingResult.rejectValue(
                        "recipientArkAddress",
                        FieldErrorCodes.INVALID_ARK_ADDRESS_CHECKSUM,
                        "Invalid ARK address checksum."
                    );
                } else {
                    bindingResult.rejectValue(
                        "recipientArkAddress",
                        FieldErrorCodes.INVALID_ARK_ADDRESS,
                        "Invalid ARK address."
                    );
                }
            }
        }

        String returnBtcAddress = createContractRequest.getArguments().getReturnBtcAddress();
        if (! StringUtils.isEmpty(returnBtcAddress)) {
            try {
                new Address(MainNetParams.get(), Hex.decode(returnBtcAddress));
            } catch (AddressFormatException e) {
                bindingResult.rejectValue(
                    "returnBtcAddress",
                    FieldErrorCodes.INVALID_BTC_ADDRESS_CHECKSUM,
                    "Invalid BTC address checksum."
                );
            } catch (DecoderException e) {
                bindingResult.rejectValue(
                    "returnBtcAddress",
                    FieldErrorCodes.INVALID_BTC_ADDRESS,
                    "Invalid BTC address."
                );
            }
        }

        if (bindingResult.hasErrors()) {
            throw new ValidatorException(bindingResult);
        }
    }
    
}
