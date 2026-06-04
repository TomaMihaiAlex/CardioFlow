#include "ble_server.h"
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Arduino.h>

CardioFlowBleServer bleServer;

class ServerCallbacks : public BLEServerCallbacks {
public:
    void onConnect(BLEServer* server) override {
        Serial.println("BLE: telefon conectat");
    }
    void onDisconnect(BLEServer* server) override {
        Serial.println("BLE: telefon deconectat, reincep advertising...");
        BLEDevice::startAdvertising();
    }
};

// Stored as a member to avoid heap leak on re-init
static ServerCallbacks sServerCallbacks;

void CardioFlowBleServer::begin(const char* deviceName) {
    _mutex = xSemaphoreCreateMutex();

    BLEDevice::init(deviceName);
    _server = BLEDevice::createServer();
    _server->setCallbacks(&sServerCallbacks);

    BLEService* service = _server->createService(SERVICE_UUID);

    _charMeasurement = service->createCharacteristic(
        CHAR_MEASUREMENT, BLECharacteristic::PROPERTY_NOTIFY);
    _charMeasurement->addDescriptor(new BLE2902());

    _charEcg = service->createCharacteristic(
        CHAR_ECG, BLECharacteristic::PROPERTY_NOTIFY);
    _charEcg->addDescriptor(new BLE2902());

    _charAlert = service->createCharacteristic(
        CHAR_ALERT, BLECharacteristic::PROPERTY_NOTIFY);
    _charAlert->addDescriptor(new BLE2902());

    thresholdsCharacteristic = service->createCharacteristic(
        CHAR_THRESHOLDS, BLECharacteristic::PROPERTY_WRITE);

    service->start();

    BLEAdvertising* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    // 0x06 = 7.5 ms minimum connection interval (6 × 1.25 ms)
    advertising->setMinPreferred(0x06);
    BLEDevice::startAdvertising();

    Serial.printf("BLE: advertising ca \"%s\"\n", deviceName);
}

void CardioFlowBleServer::notifyMeasurement(const char* json) {
    if (!isConnected()) return;
    xSemaphoreTake(_mutex, portMAX_DELAY);
    _charMeasurement->setValue(reinterpret_cast<const uint8_t*>(json), strlen(json));
    _charMeasurement->notify();
    xSemaphoreGive(_mutex);
}

void CardioFlowBleServer::notifyEcg(const uint16_t* samples, size_t count) {
    if (!isConnected()) return;
    if (count > BLE_MAX_ECG_SAMPLES) count = BLE_MAX_ECG_SAMPLES;

    // Fixed-size stack buffer — no VLA, capped to BLE_MAX_ECG_SAMPLES
    uint8_t buf[BLE_MAX_ECG_SAMPLES * 2];
    for (size_t i = 0; i < count; i++) {
        buf[i * 2]     = (samples[i] >> 8) & 0xFF;
        buf[i * 2 + 1] = samples[i] & 0xFF;
    }

    xSemaphoreTake(_mutex, portMAX_DELAY);
    _charEcg->setValue(buf, count * 2);
    _charEcg->notify();
    xSemaphoreGive(_mutex);
}

void CardioFlowBleServer::notifyAlert(const char* json) {
    if (!isConnected()) return;
    xSemaphoreTake(_mutex, portMAX_DELAY);
    _charAlert->setValue(reinterpret_cast<const uint8_t*>(json), strlen(json));
    _charAlert->notify();
    xSemaphoreGive(_mutex);
}

bool CardioFlowBleServer::isConnected() const {
    return _server != nullptr && _server->getConnectedCount() > 0;
}
