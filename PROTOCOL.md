# CardioFlow — Protocol BLE Embedded ↔ Mobile

> Document comun pentru echipele embedded, mobile și cloud.
> Ultima actualizare: 2026-06-04

---

## 1. Prezentare generală

Modulul embedded (ESP32) comunică cu aplicația mobilă exclusiv prin **Bluetooth Low Energy (BLE)**.
Datele sunt organizate într-un singur serviciu BLE cu 4 caracteristici distincte.

---

## 2. Serviciu BLE

**Service UUID:** `12345678-1234-1234-1234-123456789ABC`

| Caracteristică   | UUID                                   | Tip    | Direcție          | Format       | Frecvență          |
|------------------|----------------------------------------|--------|-------------------|--------------|--------------------|
| Measurement      | `12345678-1234-1234-1234-000000000001` | NOTIFY | ESP32 → Telefon   | JSON string  | la fiecare 10s     |
| ECG Stream       | `12345678-1234-1234-1234-000000000002` | NOTIFY | ESP32 → Telefon   | binar uint16 | continuu ~250Hz    |
| Alert            | `12345678-1234-1234-1234-000000000003` | NOTIFY | ESP32 → Telefon   | JSON string  | la eveniment       |
| Thresholds       | `12345678-1234-1234-1234-000000000004` | WRITE  | Telefon → ESP32   | JSON string  | la modificare      |

---

## 3. Formate de pachete

### 3.1 Measurement (UUID ...0001)

Trimis la fiecare 10 secunde. Conține valorile medii ale citirii.

```json
{
  "patientId": "1",
  "timestamp": "2026-06-04T12:00:00Z",
  "heartRate": 75,
  "spo2": 98,
  "temperature": 36.6,
  "humidity": 42.5,
  "ecgSamples": [512, 515, 508, 520, 498, 505]
}
```

| Câmp         | Tip      | Unitate | Descriere                              |
|--------------|----------|---------|----------------------------------------|
| patientId    | string   | —       | ID-ul pacientului (primit din thresholds) |
| timestamp    | string   | ISO 8601| Ora citirii (UTC)                      |
| heartRate    | int      | BPM     | Puls mediu pe intervalul de 10s        |
| spo2         | int      | %       | Saturație oxigen medie                 |
| temperature  | double   | °C      | Temperatura ambientală                 |
| humidity     | double   | %       | Umiditatea ambientală                  |
| ecgSamples   | int[]    | ADC raw | Mostre ECG colectate în intervalul 10s |

### 3.2 ECG Stream (UUID ...0002)

Stream continuu de mostre ADC de la senzorul AD8232.
Format binar: pachete de **20 octeți** = 10 mostre × uint16 (big-endian).

```
[sample0_hi][sample0_lo][sample1_hi][sample1_lo]...[sample9_hi][sample9_lo]
```

- Frecvență de eșantionare: ~250Hz
- Valori: 0–4095 (ADC 12-bit al ESP32)
- Mobil-ul reconstituie graficul ECG din stream continuu

### 3.3 Alert (UUID ...0003)

Trimis imediat când o valoare depășește limitele din Thresholds.

```json
{
  "patientId": "1",
  "type": "high_heart_rate",
  "value": 125,
  "timestamp": "2026-06-04T12:00:05Z",
  "severity": "high"
}
```

| Câmp      | Valori posibile pentru `type`                                     |
|-----------|-------------------------------------------------------------------|
| type      | `high_heart_rate`, `low_heart_rate`, `low_spo2`, `high_temp`, `low_temp` |
| severity  | `high` (puls/SpO2 critic), `medium` (temperatură)                |

### 3.4 Thresholds (UUID ...0004)

Primit de la telefon. ESP32-ul stochează valorile și le aplică imediat.

```json
{
  "patientId": "1",
  "hrMin": 50,
  "hrMax": 100,
  "spo2Min": 90,
  "tempMin": 35.5,
  "tempMax": 37.5,
  "humMin": 30,
  "humMax": 70,
  "persistSeconds": 10,
  "activityIntervalMinutes": 5
}
```

---

## 4. Flux de lucru ESP32

```
┌─────────────────────────────────────────────────────────┐
│  Task principal (loop 10s)                              │
│  1. Citeşte MAX30102 → HR + SpO2                        │
│  2. Citeşte DHT11 → temperatură + umiditate             │
│  3. Calculează medii                                    │
│  4. Verifică thresholds → dacă depăşit → NOTIFY Alert   │
│  5. Construieşte Measurement + ecgSamples din buffer    │
│  6. NOTIFY Measurement                                  │
│  7. Intră în light sleep până la următoarea citire      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Task paralel ECG (FreeRTOS task)                       │
│  1. Citeşte ADC de la AD8232 la ~250Hz                  │
│  2. Acumulează 10 mostre                                │
│  3. NOTIFY ECG Stream (20 bytes)                        │
│  4. Stochează ultimele N mostre în buffer circular      │
│     (folosit de task-ul principal pentru ecgSamples)    │
└─────────────────────────────────────────────────────────┘
```

---

## 5. Structura proiectului embedded

```
embedded/
├── platformio.ini
├── src/
│   ├── main.cpp
│   ├── sensors/
│   │   ├── max30102.cpp / max30102.h
│   │   ├── ad8232.cpp   / ad8232.h
│   │   └── dht11.cpp    / dht11.h
│   ├── ble/
│   │   ├── ble_server.cpp / ble_server.h
│   │   └── ble_callbacks.cpp / ble_callbacks.h
│   ├── data/
│   │   ├── measurement.h
│   │   └── thresholds.h
│   └── alerts/
│       └── alert_manager.cpp / alert_manager.h
└── lib/
```

---

## 6. Ce trebuie modificat pe mobile (echipa Alex-mobile)

### 6.1 `Measurement.java` — adaugă câmpul ECG

```java
// Adaugă în clasa Measurement:
private List<Integer> ecgSamples;

public List<Integer> getEcgSamples() { return ecgSamples; }
public void setEcgSamples(List<Integer> ecgSamples) { this.ecgSamples = ecgSamples; }
```

### 6.2 BLE — ce trebuie implementat pe Android

Aplicația mobilă trebuie să:

1. **Scaneze** după device-uri BLE cu Service UUID `12345678-1234-1234-1234-123456789ABC`
2. **Se conecteze** la ESP32 (device name: `CardioFlow`)
3. **Subscribe** (enable notifications) pe caracteristicile:
   - `...0001` → parsează JSON → actualizează UI Measurement
   - `...0002` → parsează binar uint16[] → actualizează grafic ECG în timp real
   - `...0003` → parsează JSON → afișează alertă
4. **Scrie** pe `...0004` când utilizatorul modifică thresholds-urile

### 6.3 Thresholds — aliniere JSON

Câmpurile din `patients_data.json` folosesc snake_case (`hr_min`), dar `Thresholds.java` folosește camelCase (`hrMin`).
Recomandare: adaugă `@SerializedName` annotations sau standardizează pe camelCase peste tot.

---

## 7. Librării recomandate pentru embedded (PlatformIO)

| Librărie                  | Scop                        |
|---------------------------|-----------------------------|
| `ESP32 BLE Arduino`       | BLE server + GATT           |
| `adafruit/DHT sensor library` | Citire DHT11            |
| `sparkfun/SparkFun MAX3010x Pulse and Proximity Sensor Library` | HR + SpO2 |
| `ArduinoJson`             | Serializare JSON            |

---

## 8. Limite hardware de reținut

- MTU BLE implicit: **23 bytes** (poate fi negociat până la ~512 bytes)
- Pachetele JSON Measurement pot depăși MTU-ul implicit dacă `ecgSamples` are multe elemente — se recomandă max 20 mostre per pachet Measurement
- ECG stream e separat tocmai pentru a evita această limitare
- AD8232 produce semnal analogic — citit prin ADC GPIO (pin configurat în `ad8232.h`)
