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
    bool isPresent() const { return _present; }

private:
    bool _present = false;   // true doar dacă begin() a găsit senzorul pe I2C
};
