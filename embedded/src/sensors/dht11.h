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
