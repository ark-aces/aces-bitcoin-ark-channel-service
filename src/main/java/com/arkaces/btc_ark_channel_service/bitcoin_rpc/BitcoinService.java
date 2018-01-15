package com.arkaces.btc_ark_channel_service.bitcoin_rpc;

import com.arkaces.aces_server.common.json.NiceObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class BitcoinService {

    private final RestTemplate bitcoinRpcRestTemplate;

    private final BitcoinRpcRequestFactory bitcoinRpcRequestFactory = new BitcoinRpcRequestFactory();

    private final NiceObjectMapper objectMapper = new NiceObjectMapper(new ObjectMapper());

    public String sendTransaction(String recipientAddress, BigDecimal amount) {
        HttpEntity<String> blockHashRequestEntity = getRequestEntity("sendtoaddress", new ArrayList<>());
        return bitcoinRpcRestTemplate
                .exchange(
                        "/",
                        HttpMethod.POST,
                        blockHashRequestEntity,
                        new ParameterizedTypeReference<BitcoinRpcResponse<String>>() {}
                )
                .getBody()
                .getResult();
    }

    public String getNewAddress() {
        HttpEntity<String> blockHashRequestEntity = getRequestEntity("getnewaddress", new ArrayList<>());
        return bitcoinRpcRestTemplate
            .exchange(
                "/",
                HttpMethod.POST,
                blockHashRequestEntity,
                new ParameterizedTypeReference<BitcoinRpcResponse<String>>() {}
            )
            .getBody()
            .getResult();
    }

    public String getPrivateKey(String address) {
        HttpEntity<String> blockHashRequestEntity = getRequestEntity("dumpprivkey", Arrays.asList(address));
        return bitcoinRpcRestTemplate
            .exchange(
                "/",
                HttpMethod.POST,
                blockHashRequestEntity,
                new ParameterizedTypeReference<BitcoinRpcResponse<String>>() {}
            )
            .getBody()
            .getResult();
    }

    public JsonNode getTransaction(String transactionId) {
        HttpEntity<String> blockHashRequestEntity = getRequestEntity("gettransaction", Arrays.asList(transactionId));
        return bitcoinRpcRestTemplate
            .exchange(
                "/",
                HttpMethod.POST,
                blockHashRequestEntity,
                new ParameterizedTypeReference<BitcoinRpcResponse<JsonNode>>() {}
            )
            .getBody()
            .getResult();
    }


    private HttpEntity<String> getRequestEntity(String method, List<Object> params) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");

        BitcoinRpcRequest bitcoinRpcRequest = bitcoinRpcRequestFactory.create(method, params);
        String body = objectMapper.writeValueAsString(bitcoinRpcRequest);

        return new HttpEntity<>(body, headers);
    }

}
