#pragma once
#include <stdint.h>
#include <DHT.h>

struct DhtReading {
    float temperature;
    float humidity;
    bool valid;
};

class Dht11Sensor {
public:
    explicit Dht11Sensor(uint8_t pin);
    ~Dht11Sensor();
    void begin();
    // NOTE: not thread-safe. Must be called from one FreeRTOS task only.
    DhtReading read();

private:
    uint8_t _pin;
    DHT*    _dht = nullptr;
};
