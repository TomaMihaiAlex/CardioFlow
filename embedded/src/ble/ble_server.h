#pragma once
#include <BLEServer.h>
#include <BLECharacteristic.h>
#include <stdint.h>

#define SERVICE_UUID     "12345678-1234-1234-1234-123456789ABC"
#define CHAR_MEASUREMENT "12345678-1234-1234-1234-000000000001"
#define CHAR_ECG         "12345678-1234-1234-1234-000000000002"
#define CHAR_ALERT       "12345678-1234-1234-1234-000000000003"
#define CHAR_THRESHOLDS  "12345678-1234-1234-1234-000000000004"

class CardioFlowBleServer {
public:
    void begin(const char* deviceName);
    void notifyMeasurement(const char* json);
    void notifyEcg(const uint16_t* samples, size_t count);
    void notifyAlert(const char* json);
    bool isConnected() const;

    BLECharacteristic* thresholdsCharacteristic;

private:
    BLEServer*         _server;
    BLECharacteristic* _charMeasurement;
    BLECharacteristic* _charEcg;
    BLECharacteristic* _charAlert;
};

extern CardioFlowBleServer bleServer;
