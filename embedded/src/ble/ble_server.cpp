#include "ble_server.h"
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Arduino.h>

CardioFlowBleServer bleServer;

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* server) override {
        Serial.println("BLE: telefon conectat");
    }
    void onDisconnect(BLEServer* server) override {
        Serial.println("BLE: telefon deconectat, reincep advertising...");
        BLEDevice::startAdvertising();
    }
};

void CardioFlowBleServer::begin(const char* deviceName) {
    BLEDevice::init(deviceName);
    _server = BLEDevice::createServer();
    _server->setCallbacks(new ServerCallbacks());

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
    advertising->setMinPreferred(0x06);
    BLEDevice::startAdvertising();

    Serial.printf("BLE: advertising ca \"%s\"\n", deviceName);
}

void CardioFlowBleServer::notifyMeasurement(const char* json) {
    if (!isConnected()) return;
    _charMeasurement->setValue((uint8_t*)json, strlen(json));
    _charMeasurement->notify();
}

void CardioFlowBleServer::notifyEcg(const uint16_t* samples, size_t count) {
    if (!isConnected()) return;
    // Serialise as big-endian pairs: [hi][lo] per sample
    uint8_t buf[count * 2];
    for (size_t i = 0; i < count; i++) {
        buf[i * 2]     = (samples[i] >> 8) & 0xFF;
        buf[i * 2 + 1] = samples[i] & 0xFF;
    }
    _charEcg->setValue(buf, count * 2);
    _charEcg->notify();
}

void CardioFlowBleServer::notifyAlert(const char* json) {
    if (!isConnected()) return;
    _charAlert->setValue((uint8_t*)json, strlen(json));
    _charAlert->notify();
}

bool CardioFlowBleServer::isConnected() const {
    return _server != nullptr && _server->getConnectedCount() > 0;
}
