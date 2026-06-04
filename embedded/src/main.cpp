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
static Dht11Sensor    dht(DHT_PIN);
static Max30102Sensor pulse;
static Ad8232Sensor   ecg(ECG_OUTPUT_PIN, ECG_LO_PLUS, ECG_LO_MINUS);

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
        DhtReading dhtR = dht.read();
        m.temperature = dhtR.temperature;
        m.humidity    = dhtR.humidity;
        if (!dhtR.valid) m.valid = false;

        PulseReading pulseR = pulse.read();
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
        uint16_t sample = ecg.isLeadOff() ? 0 : ecg.readSample();

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

    dht.begin();
    ecg.begin();

    if (!pulse.begin()) {
        Serial.println("EROARE: MAX30102 nu a fost gasit! Verifica I2C: SDA=21, SCL=22.");
    }

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
