#include "dht11.h"
#include <DHT.h>
#include "../config.h"

static DHT* _dht = nullptr;

Dht11Sensor::Dht11Sensor(uint8_t pin) : _pin(pin) {}

void Dht11Sensor::begin() {
    _dht = new DHT(_pin, DHT_SENSOR_TYPE);
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
