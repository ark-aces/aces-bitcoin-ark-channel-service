# aces-service-btc-ark-channel
ACES BTC to ARK transfer channel service


## Using Service

Get service info:

```
curl http://localhost:9190/
```
```
{
  "name" : "Aces BTC-ARK Channel Service",
  "description" : "ACES BTC to ARK Channel service for transferring BTC to ARK",
  "version" : "1.0.0",
  "websiteUrl" : "https://arkaces.com",
  "instructions" : "After this contract is executed, any BTC sent to depositBtcAddress will be exchanged for ARK and  sent directly to the given recipientArkAddress less service fees.\n",
  "flatFee" : "0",
  "percentFee" : "1.00%",
  "capacities": [{
    "value": "50.00",
    "unit": "BTC",
    "displayValue": "50 BTC"
  }],
  "inputSchema" : {
    "type" : "object",
    "properties" : {
      "recipientArkAddress" : {
        "type" : "string"
      }
    },
    "required" : [ "recipientArkAddress" ]
  },
  "outputSchema" : {
    "type" : "object",
    "properties" : {
      "depositBtcAddress" : {
        "type" : "string"
      },
      "recipientArkAddress" : {
        "type" : "string"
      },
      "transfers" : {
        "type" : "array",
        "properties" : {
          "btcAmount" : {
            "type" : "string"
          },
          "btcToArkRate" : {
            "type" : "string"
          },
          "btcFlatFee" : {
            "type" : "string"
          },
          "btcPercentFee" : {
            "type" : "string"
          },
          "btcTotalFee" : {
            "type" : "string"
          },
          "arkSendAmount" : {
            "type" : "string"
          },
          "arkTransactionId" : {
            "type" : "string"
          },
          "createdAt" : {
            "type" : "string"
          }
        }
      }
    }
  }
}
```

Create a new Service Contract:

```
curl -X POST localhost:9190/contracts \
-H 'Content-type: application/json' \
-d '{
  "recipientArkAddress": "ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx"
}' 
```

```
{
  "id": "abe05cd7-40c2-4fb0-a4a7-8d2f76e74978",
  "createdAt": "2017-07-04T21:59:38.129Z",
  "correlationId": "4aafe9-4a40-a7fb-6e788d2497f7",
  "status": "executed",
  "results": {
  
    "recipientArkAddress": "ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx",
    "depositBtcAddress": "5b83337a5af30bba26a55830a7d0ccf69114137ff699a3d718699ba1f498d77b",
    "transfers": []
}
```

Get Contract information after sending BTC funds to `depositBtcAddress`:

```
curl -X GET http://localhost:9190/contracts/{id}
```

```
{
  "id": "abe05cd7-40c2-4fb0-a4a7-8d2f76e74978",
  "createdAt": "2017-07-04T21:59:38.129Z",
  "correlationId": "4aafe9-4a40-a7fb-6e788d2497f7",
  "status": "executed",
  "results": {
    "recipientArkAddress": "ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx",
    "depositBtcAddress": "5b83337a5af30bba26a55830a7d0ccf69114137ff699a3d718699ba1f498d77b",
    "transfers": [
      "id": "fa046b0e-7b05-4a2d-a4c9-168951df3b90",
      "createdAt": "2017-07-05T21:00:38.457Z",
      "btcTransactionId": "",
      "btcAmount": "1.00000",
      "btcToArkRate": "2027.58000",
      "btcFlatFee": "0.00000",
      "btcPercentFee": "1.00000",
      "btcTotalFee": "0.01000",
      "arkSendAmount": "2007.30420",
      "arkTransactionId": "49f55381c5c3c70f96e848df53ab7f9ae9881dbb8eb43e8f91f642018bf1258f"
    ]
}
```