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

    // Use !isNull() + as<float/int>() to accept both integer and decimal JSON
    // (e.g. "hrMin": 60 AND "hrMin": 60.0) — is<int>() alone rejects decimals.
    if (!doc["hrMin"].isNull() && !doc["hrMax"].isNull()) {
        int newMin = doc["hrMin"].as<int>();
        int newMax = doc["hrMax"].as<int>();
        if (newMin < 20 || newMax > 300 || newMin >= newMax) {
            Serial.printf("Thresholds: HR range [%d-%d] invalid (must be 20-300, min<max), skipping\n", newMin, newMax);
        } else {
            _target->hrMin = newMin;
            _target->hrMax = newMax;
        }
    } else {
        if (!doc["hrMin"].isNull()) {
            int newMin = doc["hrMin"].as<int>();
            if (newMin < 20 || newMin >= _target->hrMax) {
                Serial.printf("Thresholds: hrMin (%d) invalid, skipping\n", newMin);
            } else {
                _target->hrMin = newMin;
            }
        }
        if (!doc["hrMax"].isNull()) {
            int newMax = doc["hrMax"].as<int>();
            if (newMax > 300 || _target->hrMin >= newMax) {
                Serial.printf("Thresholds: hrMax (%d) invalid, skipping\n", newMax);
            } else {
                _target->hrMax = newMax;
            }
        }
    }

    if (!doc["spo2Min"].isNull()) {
        int newSpo2 = doc["spo2Min"].as<int>();
        if (newSpo2 < 50 || newSpo2 > 100) {
            Serial.printf("Thresholds: spo2Min (%d) out of range [50-100], skipping\n", newSpo2);
        } else {
            _target->spo2Min = newSpo2;
        }
    }

    if (!doc["tempMin"].isNull() && !doc["tempMax"].isNull()) {
        float newTMin = doc["tempMin"].as<float>();
        float newTMax = doc["tempMax"].as<float>();
        if (newTMin < 10.0f || newTMax > 50.0f || newTMin >= newTMax) {
            Serial.printf("Thresholds: Temp range [%.1f-%.1f] invalid (10-50, min<max), skipping\n", newTMin, newTMax);
        } else {
            _target->tempMin = newTMin;
            _target->tempMax = newTMax;
        }
    } else {
        if (!doc["tempMin"].isNull()) {
            float v = doc["tempMin"].as<float>();
            if (v >= 10.0f && v < _target->tempMax) _target->tempMin = v;
        }
        if (!doc["tempMax"].isNull()) {
            float v = doc["tempMax"].as<float>();
            if (v <= 50.0f && v > _target->tempMin) _target->tempMax = v;
        }
    }

    if (!doc["humMin"].isNull() && !doc["humMax"].isNull()) {
        float newHMin = doc["humMin"].as<float>();
        float newHMax = doc["humMax"].as<float>();
        if (newHMin < 0.0f || newHMax > 100.0f || newHMin >= newHMax) {
            Serial.printf("Thresholds: Hum range [%.1f-%.1f] invalid (0-100, min<max), skipping\n", newHMin, newHMax);
        } else {
            _target->humMin = newHMin;
            _target->humMax = newHMax;
        }
    } else {
        if (!doc["humMin"].isNull()) {
            float v = doc["humMin"].as<float>();
            if (v >= 0.0f && v < _target->humMax) _target->humMin = v;
        }
        if (!doc["humMax"].isNull()) {
            float v = doc["humMax"].as<float>();
            if (v <= 100.0f && v > _target->humMin) _target->humMax = v;
        }
    }

    if (!doc["persistSeconds"].isNull()) {
        int v = doc["persistSeconds"].as<int>();
        if (v > 0) _target->persistSeconds = v;
        else Serial.printf("Thresholds: persistSeconds (%d) must be > 0, skipping\n", v);
    }

    if (!doc["activityIntervalMinutes"].isNull()) {
        int v = doc["activityIntervalMinutes"].as<int>();
        if (v > 0) _target->activityIntervalMinutes = v;
        else Serial.printf("Thresholds: activityIntervalMinutes (%d) must be > 0, skipping\n", v);
    }

    Serial.printf("Thresholds write processed: HR=%d-%d, SpO2>=%d, Temp=%.1f-%.1f\n",
        _target->hrMin, _target->hrMax, _target->spo2Min,
        _target->tempMin, _target->tempMax);
}
