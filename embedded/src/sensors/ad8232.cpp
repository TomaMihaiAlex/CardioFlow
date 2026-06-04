#include "ad8232.h"
#include <Arduino.h>

Ad8232Sensor::Ad8232Sensor(uint8_t outputPin, uint8_t loPlus, uint8_t loMinus)
    : _outputPin(outputPin), _loPlus(loPlus), _loMinus(loMinus) {}

void Ad8232Sensor::begin() {
    pinMode(_loPlus,  INPUT);
    pinMode(_loMinus, INPUT);
    // Configure ADC for full 0-3.3V range and guaranteed 12-bit resolution.
    // Without ADC_11db attenuation, ESP32 clips signals above ~1.1V — AD8232
    // output swings 0-3.3V so ECG waveforms would be severely clipped.
    analogReadResolution(12);
    analogSetPinAttenuation(_outputPin, ADC_11db);
}

uint16_t Ad8232Sensor::readSample() const {
    // ESP32 ADC is 12-bit: range 0–4095
    return (uint16_t)analogRead(_outputPin);
}

bool Ad8232Sensor::isLeadOff() const {
    // AD8232 LO+ and LO- each go HIGH when their electrode is disconnected.
    // Either electrode missing = lead off (safety: use OR).
    return (digitalRead(_loPlus) == 1) || (digitalRead(_loMinus) == 1);
}
