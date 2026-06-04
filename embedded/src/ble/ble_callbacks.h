#pragma once
#include <BLECharacteristic.h>
#include <freertos/semphr.h>
#include "../data/thresholds.h"

// NOTE: onWrite() is called from the BLE stack's own RTOS task.
// _target is shared with the main task — protected by _mutex.
class ThresholdsCallback : public BLECharacteristicCallbacks {
public:
    ThresholdsCallback(Thresholds* target, SemaphoreHandle_t mutex);
    void onWrite(BLECharacteristic* characteristic) override;

private:
    Thresholds*       _target;
    SemaphoreHandle_t _mutex;
};
