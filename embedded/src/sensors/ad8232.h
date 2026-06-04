#pragma once
#include <stdint.h>

class Ad8232Sensor {
public:
    Ad8232Sensor(uint8_t outputPin, uint8_t loPlus, uint8_t loMinus);
    void begin();
    uint16_t readSample();   // returns ADC value 0-4095 (ESP32 12-bit ADC)
    bool isLeadOff();        // true = electrodes not connected

private:
    uint8_t _outputPin;
    uint8_t _loPlus;
    uint8_t _loMinus;
};
