#include "ad8232.h"
#include <Arduino.h>

Ad8232Sensor::Ad8232Sensor(uint8_t outputPin, uint8_t loPlus, uint8_t loMinus)
    : _outputPin(outputPin), _loPlus(loPlus), _loMinus(loMinus) {}

void Ad8232Sensor::begin() {
    pinMode(_loPlus,  INPUT);
    pinMode(_loMinus, INPUT);
    // _outputPin is ADC — no pinMode needed for analogRead
}

uint16_t Ad8232Sensor::readSample() {
    // ESP32 ADC is 12-bit: range 0–4095
    return (uint16_t)analogRead(_outputPin);
}

bool Ad8232Sensor::isLeadOff() {
    // AD8232 LO+ and LO- both go HIGH when an electrode is disconnected
    return (digitalRead(_loPlus) == 1) || (digitalRead(_loMinus) == 1);
}
