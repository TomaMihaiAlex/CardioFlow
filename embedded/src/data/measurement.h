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
