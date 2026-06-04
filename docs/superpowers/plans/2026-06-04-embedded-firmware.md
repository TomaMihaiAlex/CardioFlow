# CardioFlow Embedded Firmware Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Firmware complet pentru ESP32 care citește MAX30102, AD8232, DHT11 și trimite datele pe telefon prin BLE conform PROTOCOL.md.

**Architecture:** Două task-uri FreeRTOS — task principal (loop 10s: HR/SpO2/temp/humidity → JSON → BLE NOTIFY) și task ECG (continuu ~250Hz ADC → binar BLE NOTIFY). BLE server GATT cu 4 caracteristici (Measurement, ECG, Alert, Thresholds).

**Tech Stack:** ESP32, PlatformIO + Arduino framework, SparkFun MAX3010x, Adafruit DHT, ArduinoJson v7, ESP32 BLE Arduino (inclus în framework)

---

## Structura fișierelor

```
embedded/
├── platformio.ini
├── src/
│   ├── config.h                  ← pini GPIO, constante
│   ├── main.cpp                  ← setup() + 2 FreeRTOS tasks
│   ├── sensors/
│   │   ├── dht11.h / dht11.cpp
│   │   ├── max30102.h / max30102.cpp
│   │   └── ad8232.h / ad8232.cpp
│   ├── ble/
│   │   ├── ble_server.h / ble_server.cpp
│   │   └── ble_callbacks.h / ble_callbacks.cpp
│   ├── data/
│   │   ├── measurement.h
│   │   └── thresholds.h
│   └── alerts/
│       ├── alert_manager.h
│       └── alert_manager.cpp
└── test/
    └── test_alerts/
        └── test_alert_manager.cpp
```

---

## Prerequisit: Instalare PlatformIO

Înainte de Task 1, instalează PlatformIO:
1. Deschide VS Code
2. Extensions (Ctrl+Shift+X) → caută `PlatformIO IDE` → Install
3. Repornește VS Code
4. Vei vedea iconița PlatformIO (alien cap) în sidebar stânga

---

## Task 1: Crează proiectul PlatformIO

**Files:**
- Create: `embedded/platformio.ini`

- [ ] **Step 1: Crează proiectul via PlatformIO**

Apasă iconița PlatformIO → `New Project`
- Name: `embedded`
- Board: `Espressif ESP32 Dev Module`
- Framework: `Arduino`
- Location: bifează `Use default location` SAU navighează la `d:\Desktop\EmbeddedCardioFlow\CardioFlow\`

PlatformIO creează automat structura `embedded/` cu `src/main.cpp` și `platformio.ini`.

- [ ] **Step 2: Înlocuiește conținutul `embedded/platformio.ini`**

```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
monitor_speed = 115200
lib_deps =
    adafruit/DHT sensor library@^1.4.6
    adafruit/Adafruit Unified Sensor@^1.1.14
    sparkfun/SparkFun MAX3010x Pulse and Proximity Sensor Library@^1.1.2
    bblanchon/ArduinoJson@^7.0.0
build_flags = -DCORE_DEBUG_LEVEL=3

[env:native]
platform = native
test_framework = unity
```

- [ ] **Step 3: Verifică că proiectul compilează gol**

Click pe iconița checkmark (✓) din bara de jos PlatformIO (Build) SAU `Ctrl+Alt+B`.

Expected output în terminal:
```
Building in release mode
...
RAM:   [          ]   1.3% (used 4288 bytes from 327680 bytes)
Flash: [=         ]  10.2% (used 133885 bytes from 1310720 bytes)
========================= [SUCCESS] =========================
```

- [ ] **Step 4: Commit**

```bash
git add embedded/
git commit -m "feat: init PlatformIO project for ESP32"
```

---

## Task 2: Config pini și structuri de date

**Files:**
- Create: `embedded/src/config.h`
- Create: `embedded/src/data/measurement.h`
- Create: `embedded/src/data/thresholds.h`

- [ ] **Step 1: Creează `embedded/src/config.h`**

```cpp
#pragma once

// DHT11
#define DHT_PIN         4

// AD8232 ECG
#define ECG_OUTPUT_PIN  34   // ADC input (doar citire)
#define ECG_LO_PLUS     32
#define ECG_LO_MINUS    33

// MAX30102 folosește I2C implicit: SDA=21, SCL=22

// Timing
#define MEASUREMENT_INTERVAL_MS  10000   // 10 secunde
#define ECG_SAMPLE_RATE_HZ       250
#define ECG_PACKET_SAMPLES       10      // 10 mostre per pachet BLE = 20 bytes

// BLE
#define BLE_DEVICE_NAME  "CardioFlow"

// Buffer ECG circular (stochează 1 secundă de date)
#define ECG_BUFFER_SIZE  250
```

- [ ] **Step 2: Creează `embedded/src/data/measurement.h`**

```cpp
#pragma once
#include <stdint.h>

#define ECG_SNAPSHOT_SIZE 20   // mostre ECG incluse în pachetul Measurement

struct Measurement {
    char patientId[16];
    int heartRate;        // BPM, -1 dacă senzorul nu e valid
    int spo2;             // %, -1 dacă senzorul nu e valid
    float temperature;    // °C, -1 dacă senzorul nu e valid
    float humidity;       // %, -1 dacă senzorul nu e valid
    uint16_t ecgSnapshot[ECG_SNAPSHOT_SIZE];
    bool valid;
};
```

- [ ] **Step 3: Creează `embedded/src/data/thresholds.h`**

```cpp
#pragma once

struct Thresholds {
    char patientId[16];
    int hrMin;
    int hrMax;
    int spo2Min;
    float tempMin;
    float tempMax;
    float humMin;
    float humMax;
    int persistSeconds;
    int activityIntervalMinutes;
};

// Valori default până la prima configurare de pe telefon
inline Thresholds defaultThresholds() {
    Thresholds t;
    strncpy(t.patientId, "1", sizeof(t.patientId));
    t.hrMin = 50;
    t.hrMax = 100;
    t.spo2Min = 90;
    t.tempMin = 35.5f;
    t.tempMax = 37.5f;
    t.humMin = 30.0f;
    t.humMax = 70.0f;
    t.persistSeconds = 10;
    t.activityIntervalMinutes = 5;
    return t;
}
```

- [ ] **Step 4: Build să nu aibă erori**

`Ctrl+Alt+B` — Expected: `[SUCCESS]`

- [ ] **Step 5: Commit**

```bash
git add embedded/src/config.h embedded/src/data/
git commit -m "feat: add pin config and data structs"
```

---

## Task 3: Modul DHT11

**Files:**
- Create: `embedded/src/sensors/dht11.h`
- Create: `embedded/src/sensors/dht11.cpp`

- [ ] **Step 1: Creează `embedded/src/sensors/dht11.h`**

```cpp
#pragma once
#include <stdint.h>

struct DhtReading {
    float temperature;
    float humidity;
    bool valid;
};

class Dht11Sensor {
public:
    explicit Dht11Sensor(uint8_t pin);
    void begin();
    DhtReading read();

private:
    uint8_t _pin;
};
```

- [ ] **Step 2: Creează `embedded/src/sensors/dht11.cpp`**

```cpp
#include "dht11.h"
#include <DHT.h>

static DHT* _dht = nullptr;

Dht11Sensor::Dht11Sensor(uint8_t pin) : _pin(pin) {}

void Dht11Sensor::begin() {
    _dht = new DHT(_pin, DHT11);
    _dht->begin();
}

DhtReading Dht11Sensor::read() {
    DhtReading r;
    r.temperature = _dht->readTemperature();
    r.humidity = _dht->readHumidity();
    r.valid = !isnan(r.temperature) && !isnan(r.humidity);
    if (!r.valid) {
        r.temperature = -1.0f;
        r.humidity = -1.0f;
    }
    return r;
}
```

- [ ] **Step 3: Test rapid în `main.cpp` — verifică via Serial Monitor**

Înlocuiește `embedded/src/main.cpp` temporar cu:

```cpp
#include <Arduino.h>
#include "config.h"
#include "sensors/dht11.h"

Dht11Sensor dht(DHT_PIN);

void setup() {
    Serial.begin(115200);
    dht.begin();
}

void loop() {
    DhtReading r = dht.read();
    if (r.valid) {
        Serial.printf("Temp: %.1f C  Humidity: %.1f %%\n", r.temperature, r.humidity);
    } else {
        Serial.println("DHT11: citire invalida");
    }
    delay(2000);
}
```

- [ ] **Step 4: Flash și verifică Serial Monitor**

1. Conectează ESP32 prin USB
2. Click pe iconița săgeată (→) din bara de jos PlatformIO (Upload)
3. Click pe iconița priză (Serial Monitor) din bara de jos
4. Setează baud rate: 115200

Expected output:
```
Temp: 24.0 C  Humidity: 45.0 %
Temp: 24.0 C  Humidity: 45.0 %
```

Dacă apare `DHT11: citire invalida` → verifică firul de date pe GPIO4 și rezistența pull-up 10kΩ între DATA și 3.3V.

- [ ] **Step 5: Commit**

```bash
git add embedded/src/sensors/dht11.h embedded/src/sensors/dht11.cpp
git commit -m "feat: add DHT11 sensor module"
```

---

## Task 4: Modul MAX30102

**Files:**
- Create: `embedded/src/sensors/max30102.h`
- Create: `embedded/src/sensors/max30102.cpp`

- [ ] **Step 1: Creează `embedded/src/sensors/max30102.h`**

```cpp
#pragma once

struct PulseReading {
    int heartRate;   // BPM, -1 dacă invalid
    int spo2;        // %, -1 dacă invalid
    bool valid;
};

class Max30102Sensor {
public:
    bool begin();
    PulseReading read();
};
```

- [ ] **Step 2: Creează `embedded/src/sensors/max30102.cpp`**

```cpp
#include "max30102.h"
#include <Wire.h>
#include <SparkFun_MAX3010x_Sensor_Algorithm.h>
#include <SparkFun_MAX3010x_Pulse_Oximeter.h>

static MAX30105 particleSensor;

// Buffere necesare pentru algoritmul SparkFun
static const uint8_t RATE_SIZE = 4;
static uint32_t irBuffer[100];
static uint32_t redBuffer[100];
static int32_t bufferLength = 100;
static int32_t spo2;
static int8_t validSPO2;
static int32_t heartRate;
static int8_t validHeartRate;

bool Max30102Sensor::begin() {
    if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
        return false;
    }
    particleSensor.setup();
    particleSensor.setPulseAmplitudeRed(0x0A);
    particleSensor.setPulseAmplitudeGreen(0);
    return true;
}

PulseReading Max30102Sensor::read() {
    // Colectează 100 de mostre pentru algoritm
    for (uint8_t i = 0; i < bufferLength; i++) {
        while (!particleSensor.available()) {
            particleSensor.check();
        }
        redBuffer[i] = particleSensor.getRed();
        irBuffer[i]  = particleSensor.getIR();
        particleSensor.nextSample();
    }

    maxim_heart_rate_and_oxygen_saturation(
        irBuffer, bufferLength, redBuffer,
        &spo2, &validSPO2, &heartRate, &validHeartRate
    );

    PulseReading r;
    r.valid = (validHeartRate == 1 && validSPO2 == 1 && heartRate > 20 && heartRate < 300);
    r.heartRate = r.valid ? (int)heartRate : -1;
    r.spo2      = r.valid ? (int)spo2      : -1;
    return r;
}
```

- [ ] **Step 3: Test rapid în `main.cpp`**

```cpp
#include <Arduino.h>
#include "sensors/max30102.h"

Max30102Sensor pulse;

void setup() {
    Serial.begin(115200);
    if (!pulse.begin()) {
        Serial.println("MAX30102: nu a fost gasit! Verifica conexiunea I2C.");
        while (1);
    }
    Serial.println("MAX30102: ok. Pune degetul pe senzor...");
}

void loop() {
    PulseReading r = pulse.read();
    if (r.valid) {
        Serial.printf("HR: %d BPM  SpO2: %d %%\n", r.heartRate, r.spo2);
    } else {
        Serial.println("Citire invalida - tine degetul ferm pe senzor");
    }
}
```

- [ ] **Step 4: Flash și verifică Serial Monitor**

Pune degetul pe senzor după `MAX30102: ok.`

Expected (după ~5 sec):
```
HR: 74 BPM  SpO2: 98 %
```

Dacă apare `nu a fost gasit` → verifică SDA pe GPIO21, SCL pe GPIO22, alimentare 3.3V.

- [ ] **Step 5: Commit**

```bash
git add embedded/src/sensors/max30102.h embedded/src/sensors/max30102.cpp
git commit -m "feat: add MAX30102 HR and SpO2 sensor module"
```

---

## Task 5: Modul AD8232 ECG

**Files:**
- Create: `embedded/src/sensors/ad8232.h`
- Create: `embedded/src/sensors/ad8232.cpp`

- [ ] **Step 1: Creează `embedded/src/sensors/ad8232.h`**

```cpp
#pragma once
#include <stdint.h>

class Ad8232Sensor {
public:
    Ad8232Sensor(uint8_t outputPin, uint8_t loPlus, uint8_t loMinus);
    void begin();
    uint16_t readSample();   // returnează valoare ADC 0-4095
    bool isLeadOff();        // true = electrozii nu sunt conectați

private:
    uint8_t _outputPin;
    uint8_t _loPlus;
    uint8_t _loMinus;
};
```

- [ ] **Step 2: Creează `embedded/src/sensors/ad8232.cpp`**

```cpp
#include "ad8232.h"
#include <Arduino.h>

Ad8232Sensor::Ad8232Sensor(uint8_t outputPin, uint8_t loPlus, uint8_t loMinus)
    : _outputPin(outputPin), _loPlus(loPlus), _loMinus(loMinus) {}

void Ad8232Sensor::begin() {
    pinMode(_loPlus,  INPUT);
    pinMode(_loMinus, INPUT);
    // _outputPin este ADC — nu necesită pinMode pentru citire analogică
}

uint16_t Ad8232Sensor::readSample() {
    return (uint16_t)analogRead(_outputPin);
}

bool Ad8232Sensor::isLeadOff() {
    return (digitalRead(_loPlus) == 1) || (digitalRead(_loMinus) == 1);
}
```

- [ ] **Step 3: Test rapid în `main.cpp`**

```cpp
#include <Arduino.h>
#include "config.h"
#include "sensors/ad8232.h"

Ad8232Sensor ecg(ECG_OUTPUT_PIN, ECG_LO_PLUS, ECG_LO_MINUS);

void setup() {
    Serial.begin(115200);
    ecg.begin();
    Serial.println("AD8232: ok. Ataseaza electrozii...");
}

void loop() {
    if (ecg.isLeadOff()) {
        Serial.println("Lead off - electrozi deconectati");
    } else {
        Serial.println(ecg.readSample());  // valori ~1800-2200 cu semnal ECG
    }
    delay(4);  // ~250Hz
}
```

- [ ] **Step 4: Flash și verifică Serial Monitor**

Cu electrozii atașați pe piept, expected: un flux de numere în jur de 2000 care variază ritmic cu bătăile inimii.

Fără electrozi: `Lead off - electrozi deconectati`

- [ ] **Step 5: Commit**

```bash
git add embedded/src/sensors/ad8232.h embedded/src/sensors/ad8232.cpp
git commit -m "feat: add AD8232 ECG sensor module"
```

---

## Task 6: BLE Server

**Files:**
- Create: `embedded/src/ble/ble_server.h`
- Create: `embedded/src/ble/ble_server.cpp`

- [ ] **Step 1: Creează `embedded/src/ble/ble_server.h`**

```cpp
#pragma once
#include <BLEServer.h>
#include <BLECharacteristic.h>
#include <stdint.h>

#define SERVICE_UUID     "12345678-1234-1234-1234-123456789ABC"
#define CHAR_MEASUREMENT "12345678-1234-1234-1234-000000000001"
#define CHAR_ECG         "12345678-1234-1234-1234-000000000002"
#define CHAR_ALERT       "12345678-1234-1234-1234-000000000003"
#define CHAR_THRESHOLDS  "12345678-1234-1234-1234-000000000004"

class CardioFlowBleServer {
public:
    void begin(const char* deviceName);
    void notifyMeasurement(const char* json);
    void notifyEcg(const uint16_t* samples, size_t count);
    void notifyAlert(const char* json);
    bool isConnected();

    BLECharacteristic* thresholdsCharacteristic;

private:
    BLEServer* _server;
    BLECharacteristic* _charMeasurement;
    BLECharacteristic* _charEcg;
    BLECharacteristic* _charAlert;
};

extern CardioFlowBleServer bleServer;
```

- [ ] **Step 2: Creează `embedded/src/ble/ble_server.cpp`**

```cpp
#include "ble_server.h"
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Arduino.h>

CardioFlowBleServer bleServer;

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* server) override {
        Serial.println("BLE: telefon conectat");
    }
    void onDisconnect(BLEServer* server) override {
        Serial.println("BLE: telefon deconectat, reincep advertising...");
        BLEDevice::startAdvertising();
    }
};

void CardioFlowBleServer::begin(const char* deviceName) {
    BLEDevice::init(deviceName);
    _server = BLEDevice::createServer();
    _server->setCallbacks(new ServerCallbacks());

    BLEService* service = _server->createService(SERVICE_UUID);

    // Measurement — NOTIFY
    _charMeasurement = service->createCharacteristic(
        CHAR_MEASUREMENT,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    _charMeasurement->addDescriptor(new BLE2902());

    // ECG — NOTIFY
    _charEcg = service->createCharacteristic(
        CHAR_ECG,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    _charEcg->addDescriptor(new BLE2902());

    // Alert — NOTIFY
    _charAlert = service->createCharacteristic(
        CHAR_ALERT,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    _charAlert->addDescriptor(new BLE2902());

    // Thresholds — WRITE
    thresholdsCharacteristic = service->createCharacteristic(
        CHAR_THRESHOLDS,
        BLECharacteristic::PROPERTY_WRITE
    );

    service->start();

    BLEAdvertising* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    advertising->setMinPreferred(0x06);
    BLEDevice::startAdvertising();

    Serial.printf("BLE: advertising ca \"%s\"\n", deviceName);
}

void CardioFlowBleServer::notifyMeasurement(const char* json) {
    _charMeasurement->setValue((uint8_t*)json, strlen(json));
    _charMeasurement->notify();
}

void CardioFlowBleServer::notifyEcg(const uint16_t* samples, size_t count) {
    // Serializare big-endian
    uint8_t buf[count * 2];
    for (size_t i = 0; i < count; i++) {
        buf[i * 2]     = (samples[i] >> 8) & 0xFF;
        buf[i * 2 + 1] = samples[i] & 0xFF;
    }
    _charEcg->setValue(buf, count * 2);
    _charEcg->notify();
}

void CardioFlowBleServer::notifyAlert(const char* json) {
    _charAlert->setValue((uint8_t*)json, strlen(json));
    _charAlert->notify();
}

bool CardioFlowBleServer::isConnected() {
    return _server->getConnectedCount() > 0;
}
```

- [ ] **Step 3: Test BLE în `main.cpp`**

```cpp
#include <Arduino.h>
#include "config.h"
#include "ble/ble_server.h"

void setup() {
    Serial.begin(115200);
    bleServer.begin(BLE_DEVICE_NAME);
    Serial.println("Cauta 'CardioFlow' in nRF Connect...");
}

void loop() {
    if (bleServer.isConnected()) {
        bleServer.notifyMeasurement("{\"heartRate\":75,\"spo2\":98,\"temperature\":36.6,\"humidity\":45.0}");
        Serial.println("Measurement trimis");
    }
    delay(3000);
}
```

- [ ] **Step 4: Testează cu aplicația nRF Connect**

1. Instalează **nRF Connect for Mobile** pe telefon (Google Play / App Store)
2. Flash ESP32, deschide Serial Monitor
3. În nRF Connect → Scan → caută `CardioFlow` → Connect
4. Navighează la serviciul `12345678...`
5. La caracteristica `...0001` → apasă săgeata jos (subscribe/notify)
6. La fiecare 3s vei vedea datele JSON în aplicație

Expected în Serial Monitor: `Measurement trimis`

- [ ] **Step 5: Commit**

```bash
git add embedded/src/ble/ble_server.h embedded/src/ble/ble_server.cpp
git commit -m "feat: add BLE GATT server with 4 characteristics"
```

---

## Task 7: BLE Callbacks — primire Thresholds

**Files:**
- Create: `embedded/src/ble/ble_callbacks.h`
- Create: `embedded/src/ble/ble_callbacks.cpp`

- [ ] **Step 1: Creează `embedded/src/ble/ble_callbacks.h`**

```cpp
#pragma once
#include <BLECharacteristic.h>
#include "../data/thresholds.h"

class ThresholdsCallback : public BLECharacteristicCallbacks {
public:
    explicit ThresholdsCallback(Thresholds* target);
    void onWrite(BLECharacteristic* characteristic) override;

private:
    Thresholds* _target;
};
```

- [ ] **Step 2: Creează `embedded/src/ble/ble_callbacks.cpp`**

```cpp
#include "ble_callbacks.h"
#include <ArduinoJson.h>
#include <Arduino.h>

ThresholdsCallback::ThresholdsCallback(Thresholds* target) : _target(target) {}

void ThresholdsCallback::onWrite(BLECharacteristic* characteristic) {
    std::string value = characteristic->getValue();
    if (value.empty()) return;

    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, value.c_str());
    if (err) {
        Serial.printf("Thresholds: JSON invalid (%s)\n", err.c_str());
        return;
    }

    if (doc["patientId"].is<const char*>())
        strncpy(_target->patientId, doc["patientId"], sizeof(_target->patientId) - 1);
    if (doc["hrMin"].is<int>())    _target->hrMin    = doc["hrMin"];
    if (doc["hrMax"].is<int>())    _target->hrMax    = doc["hrMax"];
    if (doc["spo2Min"].is<int>())  _target->spo2Min  = doc["spo2Min"];
    if (doc["tempMin"].is<float>()) _target->tempMin  = doc["tempMin"];
    if (doc["tempMax"].is<float>()) _target->tempMax  = doc["tempMax"];
    if (doc["humMin"].is<float>())  _target->humMin   = doc["humMin"];
    if (doc["humMax"].is<float>())  _target->humMax   = doc["humMax"];
    if (doc["persistSeconds"].is<int>()) _target->persistSeconds = doc["persistSeconds"];

    Serial.printf("Thresholds actualizate: HR=%d-%d, SpO2>=%d, Temp=%.1f-%.1f\n",
        _target->hrMin, _target->hrMax, _target->spo2Min,
        _target->tempMin, _target->tempMax);
}
```

- [ ] **Step 3: Test cu nRF Connect**

Conectează-te la `CardioFlow` în nRF Connect → caracteristica `...0004` → apasă săgeata sus (write) → trimite:
```
{"hrMin":45,"hrMax":110,"spo2Min":88,"tempMin":35.0,"tempMax":38.0,"humMin":25.0,"humMax":75.0,"persistSeconds":5}
```

Expected în Serial Monitor:
```
Thresholds actualizate: HR=45-110, SpO2>=88, Temp=35.0-38.0
```

- [ ] **Step 4: Commit**

```bash
git add embedded/src/ble/ble_callbacks.h embedded/src/ble/ble_callbacks.cpp
git commit -m "feat: add BLE thresholds write callback with JSON parsing"
```

---

## Task 8: Alert Manager

**Files:**
- Create: `embedded/src/alerts/alert_manager.h`
- Create: `embedded/src/alerts/alert_manager.cpp`
- Create: `embedded/test/test_alerts/test_alert_manager.cpp`

- [ ] **Step 1: Creează `embedded/src/alerts/alert_manager.h`**

```cpp
#pragma once
#include "../data/measurement.h"
#include "../data/thresholds.h"

struct AlertResult {
    bool triggered;
    char type[32];
    float value;
    char severity[8];  // "high" sau "medium"
};

AlertResult checkThresholds(const Measurement& m, const Thresholds& t);
void buildAlertJson(const AlertResult& alert, const char* patientId, char* outBuf, size_t bufSize);
```

- [ ] **Step 2: Creează `embedded/src/alerts/alert_manager.cpp`**

```cpp
#include "alert_manager.h"
#include <stdio.h>
#include <string.h>

AlertResult checkThresholds(const Measurement& m, const Thresholds& t) {
    AlertResult result;
    result.triggered = false;

    if (m.heartRate != -1 && m.heartRate > t.hrMax) {
        result.triggered = true;
        strncpy(result.type, "high_heart_rate", sizeof(result.type));
        result.value = m.heartRate;
        strncpy(result.severity, "high", sizeof(result.severity));
    } else if (m.heartRate != -1 && m.heartRate < t.hrMin) {
        result.triggered = true;
        strncpy(result.type, "low_heart_rate", sizeof(result.type));
        result.value = m.heartRate;
        strncpy(result.severity, "high", sizeof(result.severity));
    } else if (m.spo2 != -1 && m.spo2 < t.spo2Min) {
        result.triggered = true;
        strncpy(result.type, "low_spo2", sizeof(result.type));
        result.value = m.spo2;
        strncpy(result.severity, "high", sizeof(result.severity));
    } else if (m.temperature != -1.0f && m.temperature > t.tempMax) {
        result.triggered = true;
        strncpy(result.type, "high_temp", sizeof(result.type));
        result.value = m.temperature;
        strncpy(result.severity, "medium", sizeof(result.severity));
    } else if (m.temperature != -1.0f && m.temperature < t.tempMin) {
        result.triggered = true;
        strncpy(result.type, "low_temp", sizeof(result.type));
        result.value = m.temperature;
        strncpy(result.severity, "medium", sizeof(result.severity));
    }

    return result;
}

void buildAlertJson(const AlertResult& alert, const char* patientId, char* outBuf, size_t bufSize) {
    snprintf(outBuf, bufSize,
        "{\"patientId\":\"%s\",\"type\":\"%s\",\"value\":%.1f,\"severity\":\"%s\"}",
        patientId, alert.type, alert.value, alert.severity
    );
}
```

- [ ] **Step 3: Scrie testele Unity**

Creează `embedded/test/test_alerts/test_alert_manager.cpp`:

```cpp
#include <unity.h>
#include "../../src/alerts/alert_manager.h"
#include "../../src/data/thresholds.h"
#include <string.h>

static Thresholds makeThresholds() {
    Thresholds t;
    strncpy(t.patientId, "1", sizeof(t.patientId));
    t.hrMin = 50; t.hrMax = 100;
    t.spo2Min = 90;
    t.tempMin = 35.5f; t.tempMax = 37.5f;
    t.humMin = 30.0f; t.humMax = 70.0f;
    t.persistSeconds = 10;
    t.activityIntervalMinutes = 5;
    return t;
}

static Measurement makeMeasurement(int hr, int spo2, float temp) {
    Measurement m;
    strncpy(m.patientId, "1", sizeof(m.patientId));
    m.heartRate = hr; m.spo2 = spo2;
    m.temperature = temp; m.humidity = 45.0f;
    m.valid = true;
    return m;
}

void test_no_alert_when_normal() {
    Measurement m = makeMeasurement(75, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_FALSE(r.triggered);
}

void test_high_heart_rate_triggers_alert() {
    Measurement m = makeMeasurement(120, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("high_heart_rate", r.type);
    TEST_ASSERT_EQUAL_STRING("high", r.severity);
}

void test_low_spo2_triggers_alert() {
    Measurement m = makeMeasurement(75, 85, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("low_spo2", r.type);
}

void test_high_temp_triggers_medium_alert() {
    Measurement m = makeMeasurement(75, 98, 38.5f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("high_temp", r.type);
    TEST_ASSERT_EQUAL_STRING("medium", r.severity);
}

void test_invalid_sensor_does_not_trigger() {
    Measurement m = makeMeasurement(-1, -1, -1.0f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_FALSE(r.triggered);
}

int main() {
    UNITY_BEGIN();
    RUN_TEST(test_no_alert_when_normal);
    RUN_TEST(test_high_heart_rate_triggers_alert);
    RUN_TEST(test_low_spo2_triggers_alert);
    RUN_TEST(test_high_temp_triggers_medium_alert);
    RUN_TEST(test_invalid_sensor_does_not_trigger);
    return UNITY_END();
}
```

- [ ] **Step 4: Rulează testele native**

```bash
cd embedded
pio test -e native
```

Expected output:
```
test/test_alerts/test_alert_manager.cpp:5 tests, 0 failures
```

- [ ] **Step 5: Commit**

```bash
git add embedded/src/alerts/ embedded/test/
git commit -m "feat: add alert manager with Unity tests"
```

---

## Task 9: main.cpp — orchestrare FreeRTOS

**Files:**
- Modify: `embedded/src/main.cpp`

- [ ] **Step 1: Înlocuiește complet `embedded/src/main.cpp`**

```cpp
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
#include "alerts/alert_manager.h"

// ─── State global ──────────────────────────────────────────────
static Thresholds gThresholds = defaultThresholds();
static SemaphoreHandle_t gThresholdsMutex;

// Buffer circular ECG
static uint16_t gEcgBuffer[ECG_BUFFER_SIZE];
static volatile size_t gEcgWriteIdx = 0;
static SemaphoreHandle_t gEcgMutex;

// ─── Senzori ───────────────────────────────────────────────────
static Dht11Sensor    dht(DHT_PIN);
static Max30102Sensor pulse;
static Ad8232Sensor   ecg(ECG_OUTPUT_PIN, ECG_LO_PLUS, ECG_LO_MINUS);

// ─── Helpers ───────────────────────────────────────────────────
static void buildMeasurementJson(const Measurement& m, char* buf, size_t size) {
    JsonDocument doc;
    doc["patientId"]   = m.patientId;
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

// ─── Task principal (loop 10s) ─────────────────────────────────
static void taskMain(void* pvParameters) {
    while (true) {
        Measurement m;
        strncpy(m.patientId, gThresholds.patientId, sizeof(m.patientId));
        m.valid = true;

        // Citire senzori
        DhtReading dhtR = dht.read();
        m.temperature = dhtR.temperature;
        m.humidity    = dhtR.humidity;

        PulseReading pulseR = pulse.read();
        m.heartRate = pulseR.heartRate;
        m.spo2      = pulseR.spo2;

        // Snapshot ECG din buffer circular
        if (xSemaphoreTake(gEcgMutex, pdMS_TO_TICKS(100)) == pdTRUE) {
            for (int i = 0; i < ECG_SNAPSHOT_SIZE; i++) {
                size_t idx = (gEcgWriteIdx + ECG_BUFFER_SIZE - ECG_SNAPSHOT_SIZE + i) % ECG_BUFFER_SIZE;
                m.ecgSnapshot[i] = gEcgBuffer[idx];
            }
            xSemaphoreGive(gEcgMutex);
        }

        // Trimite Measurement
        char jsonBuf[512];
        buildMeasurementJson(m, jsonBuf, sizeof(jsonBuf));
        bleServer.notifyMeasurement(jsonBuf);
        Serial.printf("[Main] HR=%d SpO2=%d Temp=%.1f Hum=%.1f\n",
            m.heartRate, m.spo2, m.temperature, m.humidity);

        // Verifică thresholds și trimite alertă
        Thresholds localThresh;
        if (xSemaphoreTake(gThresholdsMutex, pdMS_TO_TICKS(50)) == pdTRUE) {
            localThresh = gThresholds;
            xSemaphoreGive(gThresholdsMutex);
        }

        AlertResult alert = checkThresholds(m, localThresh);
        if (alert.triggered) {
            char alertBuf[256];
            buildAlertJson(alert, m.patientId, alertBuf, sizeof(alertBuf));
            bleServer.notifyAlert(alertBuf);
            Serial.printf("[Alert] %s: %.1f (%s)\n", alert.type, alert.value, alert.severity);
        }

        vTaskDelay(pdMS_TO_TICKS(MEASUREMENT_INTERVAL_MS));
    }
}

// ─── Task ECG (continuu ~250Hz) ────────────────────────────────
static void taskEcg(void* pvParameters) {
    uint16_t packet[ECG_PACKET_SAMPLES];
    size_t packetIdx = 0;

    while (true) {
        uint16_t sample = ecg.isLeadOff() ? 0 : ecg.readSample();

        // Scrie în buffer circular
        if (xSemaphoreTake(gEcgMutex, 0) == pdTRUE) {
            gEcgBuffer[gEcgWriteIdx] = sample;
            gEcgWriteIdx = (gEcgWriteIdx + 1) % ECG_BUFFER_SIZE;
            xSemaphoreGive(gEcgMutex);
        }

        // Acumulează pachet și trimite pe BLE
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

// ─── Setup ─────────────────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    Serial.println("CardioFlow Embedded v1.0 starting...");

    gThresholdsMutex = xSemaphoreCreateMutex();
    gEcgMutex        = xSemaphoreCreateMutex();

    dht.begin();
    ecg.begin();

    if (!pulse.begin()) {
        Serial.println("EROARE: MAX30102 nu a fost gasit! Verifica conexiunea I2C.");
    }

    bleServer.begin(BLE_DEVICE_NAME);
    bleServer.thresholdsCharacteristic->setCallbacks(
        new ThresholdsCallback(&gThresholds)
    );

    xTaskCreate(taskMain, "MainTask", 8192, NULL, 1, NULL);
    xTaskCreate(taskEcg,  "EcgTask",  4096, NULL, 1, NULL);

    Serial.println("FreeRTOS tasks started. Waiting for BLE connection...");
}

void loop() {
    // FreeRTOS gestionează tot — loop() rămâne gol
    vTaskDelay(portMAX_DELAY);
}
```

- [ ] **Step 2: Build**

`Ctrl+Alt+B` — Expected: `[SUCCESS]`

Dacă există erori de compilare, verifică că toate fișierele din task-urile anterioare există.

- [ ] **Step 3: Flash pe ESP32**

Click pe → (Upload) în bara de jos PlatformIO.

- [ ] **Step 4: Verifică Serial Monitor**

Expected output la start:
```
CardioFlow Embedded v1.0 starting...
BLE: advertising ca "CardioFlow"
FreeRTOS tasks started. Waiting for BLE connection...
```

La fiecare 10s (după ce pune degetul pe MAX30102):
```
[Main] HR=74 SpO2=98 Temp=24.0 Hum=45.0
```

- [ ] **Step 5: Commit**

```bash
git add embedded/src/main.cpp
git commit -m "feat: wire up FreeRTOS tasks in main.cpp — firmware complete"
```

---

## Task 10: Test de integrare end-to-end

**Scop:** Verifică că toate cele 4 caracteristici BLE funcționează corect.

- [ ] **Step 1: Testează Measurement + ECG cu nRF Connect**

1. Conectează-te la `CardioFlow` în nRF Connect
2. Subscribe la `...0001` (Measurement) — la 10s vei vedea JSON
3. Subscribe la `...0002` (ECG) — vei vedea date binare continue
4. Verifică în Serial Monitor că `[Main]` se afișează

- [ ] **Step 2: Testează Alert**

Modifică temporar în `main.cpp` valoarea HR hardcodată la 150 pentru a forța o alertă:

```cpp
m.heartRate = 150;  // temp — forțează alertă
```

Re-flash → în nRF Connect pe `...0003` vei vedea:
```json
{"patientId":"1","type":"high_heart_rate","value":150.0,"severity":"high"}
```

Revino la codul normal și re-flash.

- [ ] **Step 3: Testează Thresholds write**

În nRF Connect → `...0004` → Write:
```
{"hrMin":45,"hrMax":110,"spo2Min":88,"tempMin":35.0,"tempMax":38.0,"humMin":25.0,"humMax":75.0,"persistSeconds":5}
```

Expected în Serial Monitor:
```
Thresholds actualizate: HR=45-110, SpO2>=88, Temp=35.0-38.0
```

- [ ] **Step 4: Commit final**

```bash
git add .
git commit -m "test: integration verified — all 4 BLE characteristics functional"
git push origin embaded
```

---

## Rezumat fișiere create

| Fișier | Responsabilitate |
|---|---|
| `embedded/platformio.ini` | Config build, librării |
| `embedded/src/config.h` | Pini GPIO, constante timing |
| `embedded/src/data/measurement.h` | Struct Measurement |
| `embedded/src/data/thresholds.h` | Struct Thresholds + default values |
| `embedded/src/sensors/dht11.cpp/h` | Citire temperatură + umiditate |
| `embedded/src/sensors/max30102.cpp/h` | Citire HR + SpO2 |
| `embedded/src/sensors/ad8232.cpp/h` | Citire ECG ADC |
| `embedded/src/ble/ble_server.cpp/h` | GATT server, 4 caracteristici |
| `embedded/src/ble/ble_callbacks.cpp/h` | Parsare JSON thresholds primit |
| `embedded/src/alerts/alert_manager.cpp/h` | Verificare limite, generare alerte |
| `embedded/src/main.cpp` | Orchestrare FreeRTOS (2 task-uri) |
| `embedded/test/test_alerts/test_alert_manager.cpp` | Unity tests logică alerte |
