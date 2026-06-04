# Design Spec — CardioFlow Embedded Module

**Data:** 2026-06-04
**Autor:** embedded team
**Status:** Aprobat

---

## Scop

Firmware pentru ESP32 care citește senzori de sănătate (MAX30102, AD8232, DHT11) și transmite datele către aplicația mobilă CardioFlow prin BLE.

---

## Arhitectură

### Structura modulelor

- `sensors/max30102` — citire HR + SpO2 via I2C, la 10s
- `sensors/ad8232` — citire ECG via ADC, continuu la ~250Hz
- `sensors/dht11` — citire temperatură + umiditate, la 10s
- `ble/ble_server` — setup GATT server, 4 caracteristici, UUID-uri definite
- `ble/ble_callbacks` — handler pentru WRITE thresholds de la telefon
- `alerts/alert_manager` — compară valori cu thresholds, emite alerte
- `data/measurement.h` — struct Measurement
- `data/thresholds.h` — struct Thresholds
- `main.cpp` — orchestrare: task principal (10s loop) + task ECG (FreeRTOS)

### Concurență

Două task-uri FreeRTOS:
1. **Task principal** — citește MAX30102 + DHT11, verifică thresholds, trimite Measurement
2. **Task ECG** — citește ADC continuu, trimite stream binar, populează buffer circular

Buffer circular ECG (dimensiune: 250 mostre = 1 secundă) partajat între task-uri cu mutex.

---

## Protocol BLE

Vezi `PROTOCOL.md` din rădăcina repo-ului pentru specificații complete de UUID-uri, formate JSON și formate binare.

---

## Gestionarea erorilor

- **Senzor MAX30102 nefuncțional** — trimite `heartRate: -1, spo2: -1` în Measurement; nu trimite alertă
- **DHT11 nefuncțional** — trimite `temperature: -1, humidity: -1`
- **Pierdere conexiune BLE** — ESP32 intră în mod advertising automat, reîncepe când telefonul se reconectează
- **Thresholds nerecepționate** — ESP32 folosește valori default hardcodate până la prima scriere

### Valori default thresholds

```
hrMin: 50, hrMax: 100
spo2Min: 90
tempMin: 35.5, tempMax: 37.5
humMin: 30, humMax: 70
persistSeconds: 10
```

---

## Economie de energie

- Light sleep între citiri (task principal)
- Task ECG activ continuu (necesar pentru grafic real-time)
- BLE advertising cu interval larg când nu e conectat

---

## Dependențe externe

| Librărie | Versiune recomandată |
|---|---|
| ESP32 BLE Arduino | latest |
| Adafruit DHT sensor library | ^1.4.0 |
| SparkFun MAX3010x Library | ^1.1.2 |
| ArduinoJson | ^7.0.0 |

---

## Criterii de succes

- [ ] Measurement trimis la fiecare 10s când e conectat BLE
- [ ] ECG stream continuu fără gap-uri vizibile pe grafic
- [ ] Alertă trimisă în <1s de la depășirea threshold-ului
- [ ] Thresholds preluate de la telefon și aplicate imediat
- [ ] Reconectare automată după pierdere conexiune
