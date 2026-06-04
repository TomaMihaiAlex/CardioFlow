#include <Arduino.h>
#include <ArduinoJson.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>

#include "config.h"
#include "data/measurement.h"
#include "data/thresholds.h"
#include "sensors/dht11.h"
#include "sensors/max30102.h"
#include "sensors/ad8232.h"
#include "ble/ble_server.h"
#include "ble/ble_callbacks.h"
#include <alert_manager.h>

// ─── Global state ───────────────────────────────────────────────
static Thresholds gThresholds = defaultThresholds();
static SemaphoreHandle_t gThresholdsMutex;

// Circular ECG buffer (stores 1 second of data)
static uint16_t gEcgBuffer[ECG_BUFFER_SIZE];
static volatile size_t gEcgWriteIdx = 0;
static SemaphoreHandle_t gEcgMutex;

// ─── Sensors ────────────────────────────────────────────────────
#ifndef MOCK_SENSORS
static Dht11Sensor    dht(DHT_PIN);
static Max30102Sensor pulse;
static Ad8232Sensor   ecg(ECG_OUTPUT_PIN, ECG_LO_PLUS, ECG_LO_MINUS);
#endif

#ifdef MOCK_SENSORS
// Generates realistic-looking fake sensor data that varies over time
static DhtReading mockDht() {
    float t = millis() / 1000.0f;
    return { 36.5f + 0.3f * sinf(t * 0.1f), 45.0f + 5.0f * sinf(t * 0.07f), true };
}
static PulseReading mockPulse() {
    float t = millis() / 1000.0f;
    int hr = 72 + (int)(8.0f * sinf(t * 0.15f));
    int sp = 98 + (int)(1.0f * sinf(t * 0.05f));
    return { hr, sp, true };
}
static uint16_t mockEcgSample() {
    // Simulated ECG: baseline + QRS spike every ~0.8s
    float t = fmodf(millis() / 1000.0f, 0.8f);
    float v = 2048.0f + 200.0f * expf(-50.0f * (t - 0.4f) * (t - 0.4f));
    return (uint16_t)constrain((int)v, 0, 4095);
}
#endif

// ─── Helpers ────────────────────────────────────────────────────
static void buildMeasurementJson(const Measurement& m, char* buf, size_t size) {
    JsonDocument doc;
    doc["patientId"]   = m.patientId;
    doc["valid"]       = m.valid;
    doc["heartRate"]   = m.heartRate;
    doc["spo2"]        = m.spo2;
    doc["temperature"] = m.temperature;
    doc["humidity"]    = m.humidity;

    JsonArray ecgArr = doc["ecgSamples"].to<JsonArray>();
    for (int i = 0; i < ECG_SNAPSHOT_SIZE; i++) {
        ecgArr.add(m.ecgSnapshot[i]);
    }
    serializeJson(doc, buf, size);
}

// ─── Main task (10s loop) ───────────────────────────────────────
static void taskMain(void* pvParameters) {
    while (true) {
        Measurement m{};
        m.valid = true;

        // Lock thresholds for patientId and threshold snapshot
        Thresholds localThresh;
        if (xSemaphoreTake(gThresholdsMutex, pdMS_TO_TICKS(100)) == pdTRUE) {
            localThresh = gThresholds;
            xSemaphoreGive(gThresholdsMutex);
        } else {
            localThresh = defaultThresholds();
        }
        strncpy(m.patientId, localThresh.patientId, sizeof(m.patientId) - 1);

        // Read sensors
#ifdef MOCK_SENSORS
        DhtReading dhtR = mockDht();
        PulseReading pulseR = mockPulse();
#else
        DhtReading dhtR = dht.read();
        PulseReading pulseR = pulse.read();
#endif
        m.temperature = dhtR.temperature;
        m.humidity    = dhtR.humidity;
        if (!dhtR.valid) m.valid = false;

        m.heartRate = pulseR.heartRate;
        m.spo2      = pulseR.spo2;
        if (!pulseR.valid) m.valid = false;

        // ECG snapshot from circular buffer
        if (xSemaphoreTake(gEcgMutex, pdMS_TO_TICKS(100)) == pdTRUE) {
            for (int i = 0; i < ECG_SNAPSHOT_SIZE; i++) {
                size_t idx = (gEcgWriteIdx + ECG_BUFFER_SIZE - ECG_SNAPSHOT_SIZE + i) % ECG_BUFFER_SIZE;
                m.ecgSnapshot[i] = gEcgBuffer[idx];
            }
            xSemaphoreGive(gEcgMutex);
        }

        // Send Measurement via BLE
        char jsonBuf[512];
        buildMeasurementJson(m, jsonBuf, sizeof(jsonBuf));
        if (bleServer.isConnected()) bleServer.notifyMeasurement(jsonBuf);
        Serial.printf("[Main] HR=%d SpO2=%d Temp=%.1f Hum=%.1f\n",
            m.heartRate, m.spo2, m.temperature, m.humidity);

        // Check thresholds and send alert if triggered
        AlertResult alert = checkThresholds(m, localThresh);
        if (alert.triggered) {
            char alertBuf[256];
            buildAlertJson(alert, m.patientId, alertBuf, sizeof(alertBuf));
            if (bleServer.isConnected()) bleServer.notifyAlert(alertBuf);
            Serial.printf("[Alert] %s: %.1f (%s)\n", alert.type, alert.value, alert.severity);
        }

        vTaskDelay(pdMS_TO_TICKS(MEASUREMENT_INTERVAL_MS));
    }
}

// ─── ECG task (continuous ~250Hz) ──────────────────────────────
static void taskEcg(void* pvParameters) {
    uint16_t packet[ECG_PACKET_SAMPLES];
    size_t packetIdx = 0;

    while (true) {
#ifdef MOCK_SENSORS
        uint16_t sample = mockEcgSample();
#else
        uint16_t sample = ecg.isLeadOff() ? 0 : ecg.readSample();
#endif

        // Write to circular buffer
        if (xSemaphoreTake(gEcgMutex, 0) == pdTRUE) {
            gEcgBuffer[gEcgWriteIdx] = sample;
            gEcgWriteIdx = (gEcgWriteIdx + 1) % ECG_BUFFER_SIZE;
            xSemaphoreGive(gEcgMutex);
        }

        // Accumulate packet and send via BLE
        packet[packetIdx++] = sample;
        if (packetIdx >= ECG_PACKET_SAMPLES) {
            if (bleServer.isConnected()) {
                bleServer.notifyEcg(packet, ECG_PACKET_SAMPLES);
            }
            packetIdx = 0;
        }

        vTaskDelay(pdMS_TO_TICKS(1000 / ECG_SAMPLE_RATE_HZ));
    }
}

// ─── Setup ──────────────────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    Serial.println("CardioFlow Embedded v1.0 starting...");

    gThresholdsMutex = xSemaphoreCreateMutex();
    gEcgMutex        = xSemaphoreCreateMutex();
    if (gThresholdsMutex == nullptr || gEcgMutex == nullptr) {
        Serial.println("FATAL: mutex creation failed — insufficient heap");
        while (1) {}
    }

#ifdef MOCK_SENSORS
    Serial.println("*** MOCK MODE — date false, niciun senzor necesar ***");
#else
    dht.begin();
    ecg.begin();
    if (!pulse.begin()) {
        Serial.println("EROARE: MAX30102 nu a fost gasit! Verifica I2C: SDA=21, SCL=22.");
    }
#endif

    bleServer.begin(BLE_DEVICE_NAME);
    bleServer.thresholdsCharacteristic->setCallbacks(
        new ThresholdsCallback(&gThresholds, gThresholdsMutex)
    );

    // EcgTask at higher priority (2) so it preempts taskMain's BLE/serial sections
    xTaskCreate(taskMain, "MainTask", 8192, NULL, 1, NULL);
    xTaskCreate(taskEcg,  "EcgTask",  6144, NULL, 2, NULL);

    Serial.println("FreeRTOS tasks started. Waiting for BLE connection...");
}

void loop() {
    // FreeRTOS manages everything — loop() stays empty
    vTaskDelay(portMAX_DELAY);
}
