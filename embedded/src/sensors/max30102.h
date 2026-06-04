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
