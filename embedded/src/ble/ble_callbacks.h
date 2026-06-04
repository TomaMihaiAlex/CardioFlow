#pragma once
#include <BLECharacteristic.h>
#include "../data/thresholds.h"

// NOTE: onWrite() is called from the BLE stack's own RTOS task, not the main task.
// _target is shared with the main task. Callers in main.cpp MUST protect all
// accesses to the shared Thresholds with a mutex. The callback intentionally does
// NOT acquire any mutex to avoid deadlock with the BLE stack's internal locks.
class ThresholdsCallback : public BLECharacteristicCallbacks {
public:
    explicit ThresholdsCallback(Thresholds* target);
    void onWrite(BLECharacteristic* characteristic) override;

private:
    Thresholds* _target;
};
