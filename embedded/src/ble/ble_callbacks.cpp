#include "ble_callbacks.h"
#include <ArduinoJson.h>
#include <Arduino.h>

ThresholdsCallback::ThresholdsCallback(Thresholds* target) : _target(target) {}

void ThresholdsCallback::onWrite(BLECharacteristic* characteristic) {
    // Guard: _target must be valid before any field access
    if (_target == nullptr) {
        Serial.println("Thresholds: _target is null, skipping write");
        return;
    }

    std::string value = characteristic->getValue();
    if (value.empty()) return;

    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, value.c_str());
    if (err) {
        Serial.printf("Thresholds: JSON invalid (%s)\n", err.c_str());
        return;
    }

    if (doc["patientId"].is<const char*>()) {
        strncpy(_target->patientId, doc["patientId"], sizeof(_target->patientId) - 1);
        // Guarantee null-termination regardless of source length
        _target->patientId[sizeof(_target->patientId) - 1] = '\0';
    }

    if (doc["hrMin"].is<int>() && doc["hrMax"].is<int>()) {
        int newMin = doc["hrMin"];
        int newMax = doc["hrMax"];
        if (newMin >= newMax) {
            Serial.printf("Thresholds: hrMin (%d) must be < hrMax (%d), skipping HR\n", newMin, newMax);
        } else {
            _target->hrMin = newMin;
            _target->hrMax = newMax;
        }
    } else {
        // Allow partial update when only one side is provided
        if (doc["hrMin"].is<int>()) {
            int newMin = doc["hrMin"];
            if (newMin >= _target->hrMax) {
                Serial.printf("Thresholds: hrMin (%d) must be < current hrMax (%d), skipping\n", newMin, _target->hrMax);
            } else {
                _target->hrMin = newMin;
            }
        }
        if (doc["hrMax"].is<int>()) {
            int newMax = doc["hrMax"];
            if (_target->hrMin >= newMax) {
                Serial.printf("Thresholds: hrMax (%d) must be > current hrMin (%d), skipping\n", newMax, _target->hrMin);
            } else {
                _target->hrMax = newMax;
            }
        }
    }

    if (doc["spo2Min"].is<int>()) {
        int newSpo2 = doc["spo2Min"];
        if (newSpo2 < 0 || newSpo2 > 100) {
            Serial.printf("Thresholds: spo2Min (%d) out of range [0-100], skipping\n", newSpo2);
        } else {
            _target->spo2Min = newSpo2;
        }
    }

    if (doc["tempMin"].is<float>() && doc["tempMax"].is<float>()) {
        float newTMin = doc["tempMin"];
        float newTMax = doc["tempMax"];
        if (newTMin >= newTMax) {
            Serial.printf("Thresholds: tempMin (%.1f) must be < tempMax (%.1f), skipping Temp\n", newTMin, newTMax);
        } else {
            _target->tempMin = newTMin;
            _target->tempMax = newTMax;
        }
    } else {
        if (doc["tempMin"].is<float>()) {
            float newTMin = doc["tempMin"];
            if (newTMin >= _target->tempMax) {
                Serial.printf("Thresholds: tempMin (%.1f) must be < current tempMax (%.1f), skipping\n", newTMin, _target->tempMax);
            } else {
                _target->tempMin = newTMin;
            }
        }
        if (doc["tempMax"].is<float>()) {
            float newTMax = doc["tempMax"];
            if (_target->tempMin >= newTMax) {
                Serial.printf("Thresholds: tempMax (%.1f) must be > current tempMin (%.1f), skipping\n", newTMax, _target->tempMin);
            } else {
                _target->tempMax = newTMax;
            }
        }
    }

    if (doc["humMin"].is<float>() && doc["humMax"].is<float>()) {
        float newHMin = doc["humMin"];
        float newHMax = doc["humMax"];
        if (newHMin >= newHMax) {
            Serial.printf("Thresholds: humMin (%.1f) must be < humMax (%.1f), skipping Hum\n", newHMin, newHMax);
        } else {
            _target->humMin = newHMin;
            _target->humMax = newHMax;
        }
    } else {
        if (doc["humMin"].is<float>()) {
            float newHMin = doc["humMin"];
            if (newHMin >= _target->humMax) {
                Serial.printf("Thresholds: humMin (%.1f) must be < current humMax (%.1f), skipping\n", newHMin, _target->humMax);
            } else {
                _target->humMin = newHMin;
            }
        }
        if (doc["humMax"].is<float>()) {
            float newHMax = doc["humMax"];
            if (_target->humMin >= newHMax) {
                Serial.printf("Thresholds: humMax (%.1f) must be > current humMin (%.1f), skipping\n", newHMax, _target->humMin);
            } else {
                _target->humMax = newHMax;
            }
        }
    }

    if (doc["persistSeconds"].is<int>())
        _target->persistSeconds = doc["persistSeconds"];

    if (doc["activityIntervalMinutes"].is<int>())
        _target->activityIntervalMinutes = doc["activityIntervalMinutes"];

    Serial.printf("Thresholds actualizate: HR=%d-%d, SpO2>=%d, Temp=%.1f-%.1f\n",
        _target->hrMin, _target->hrMax, _target->spo2Min,
        _target->tempMin, _target->tempMax);
}
