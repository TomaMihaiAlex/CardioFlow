#include "dht11.h"
#include "../config.h"

Dht11Sensor::Dht11Sensor(uint8_t pin) : _pin(pin), _dht(nullptr) {}

Dht11Sensor::~Dht11Sensor() {
    delete _dht;
}

void Dht11Sensor::begin() {
    delete _dht;  // safe on nullptr, prevents leak on re-init
    _dht = new DHT(_pin, DHT_SENSOR_TYPE);
    _dht->begin();
}

DhtReading Dht11Sensor::read() {
    DhtReading r{};
    if (_dht == nullptr) {
        r.temperature = -1.0f;
        r.humidity    = -1.0f;
        r.valid       = false;
        return r;
    }
    r.temperature = _dht->readTemperature();
    r.humidity    = _dht->readHumidity();
    r.valid = !isnan(r.temperature) && !isnan(r.humidity);
    if (!r.valid) {
        r.temperature = -1.0f;
        r.humidity    = -1.0f;
    }
    return r;
}
