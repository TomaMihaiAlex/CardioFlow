#include "max30102.h"
#include <Wire.h>
#include <MAX30105.h>
#include <spo2_algorithm.h>

static MAX30105 particleSensor;

// Static buffers are file-scope — acceptable for single-instance embedded use
// (only one MAX30102 sensor is expected per device).
static const int32_t BUFFER_LENGTH = 100;
static uint32_t irBuffer[BUFFER_LENGTH];
static uint32_t redBuffer[BUFFER_LENGTH];

bool Max30102Sensor::begin() {
    _present = particleSensor.begin(Wire, I2C_SPEED_FAST);
    if (!_present) {
        return false;
    }
    particleSensor.setup();
    particleSensor.setPulseAmplitudeRed(0x0A);
    particleSensor.setPulseAmplitudeGreen(0);
    return true;
}

// Sample collection aborts if the sensor stops producing data, so a missing or
// disconnected MAX30102 cannot wedge the caller in an infinite I2C-retry loop.
static const uint32_t SAMPLE_TIMEOUT_MS = 2000;  // ~2x the expected 1s read time

static const PulseReading kInvalidReading = { -1, -1, false };

// NOTE: read() is blocking — it collects 100 samples before returning.
// At the default sample rate this takes approximately 1 second per call.
PulseReading Max30102Sensor::read() {
    if (!_present) {
        return kInvalidReading;
    }

    uint32_t deadline = millis() + SAMPLE_TIMEOUT_MS;
    for (int32_t i = 0; i < BUFFER_LENGTH; i++) {
        while (!particleSensor.available()) {
            if ((int32_t)(millis() - deadline) >= 0) {
                // Sensor went silent (unplugged / I2C fault) — give up this read.
                _present = false;
                return kInvalidReading;
            }
            particleSensor.check();
        }
        redBuffer[i] = particleSensor.getRed();
        irBuffer[i]  = particleSensor.getIR();
        particleSensor.nextSample();
    }

    int32_t spo2 = 0; int8_t validSPO2 = 0;
    int32_t heartRate = 0; int8_t validHeartRate = 0;

    maxim_heart_rate_and_oxygen_saturation(
        irBuffer, BUFFER_LENGTH, redBuffer,
        &spo2, &validSPO2, &heartRate, &validHeartRate
    );

    PulseReading r{};
    r.valid = (validHeartRate == 1 && validSPO2 == 1 && heartRate > 20 && heartRate < 300);
    r.heartRate = r.valid ? (int)heartRate : -1;
    r.spo2      = r.valid ? (int)spo2      : -1;
    return r;
}
