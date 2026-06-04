#pragma once
#include <BLEServer.h>
#include <BLECharacteristic.h>
#include <freertos/FreeRTOS.h>
#include <freertos/semphr.h>
#include <stdint.h>

#define SERVICE_UUID     "12345678-1234-1234-1234-123456789ABC"
#define CHAR_MEASUREMENT "12345678-1234-1234-1234-000000000001"
#define CHAR_ECG         "12345678-1234-1234-1234-000000000002"
#define CHAR_ALERT       "12345678-1234-1234-1234-000000000003"
#define CHAR_THRESHOLDS  "12345678-1234-1234-1234-000000000004"

// Max ECG samples per BLE notify — enforces MTU budget (each sample = 2 bytes).
// At default ATT_MTU=23 the payload cap is 20 bytes = 10 samples.
// After MTU exchange the cap rises, but callers must not exceed this constant.
#define BLE_MAX_ECG_SAMPLES 10

class CardioFlowBleServer {
public:
    CardioFlowBleServer()
        : thresholdsCharacteristic(nullptr),
          _server(nullptr), _charMeasurement(nullptr),
          _charEcg(nullptr), _charAlert(nullptr), _mutex(nullptr) {}

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
    SemaphoreHandle_t  _mutex;
};

extern CardioFlowBleServer bleServer;
