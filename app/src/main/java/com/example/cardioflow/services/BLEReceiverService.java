package com.example.cardioflow.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.cardioflow.database.DatabaseHelper;
import com.example.cardioflow.models.Measurement;
import com.google.gson.Gson;

import java.util.UUID;

public class BLEReceiverService extends Service {
    private static final String TAG = "BLEReceiverService";
    
    // Replace with actual ESP32 Service and Characteristic UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String deviceAddress; // Should be loaded from SharedPreferences
    private Handler reconnectHandler = new Handler();
    private DatabaseHelper dbHelper;
    private Gson gson = new Gson();

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("device_address")) {
            deviceAddress = intent.getStringExtra("device_address");
            connectToDevice(deviceAddress);
        }
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null) return;
        
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server. Retrying in 5s...");
                reconnectHandler.postDelayed(() -> connectToDevice(deviceAddress), 5000);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);
                
                gatt.setCharacteristicNotification(characteristic, true);
                
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String json = new String(characteristic.getValue());
            Log.d(TAG, "Received data: " + json);
            parseAndStoreData(json);
        }
    };

    private void parseAndStoreData(String json) {
        try {
            // Assuming the JSON structure matches Measurement model or similar
            Measurement measurement = gson.fromJson(json, Measurement.class);
            // dbHelper.insertMeasurement(measurement); // Need to implement this in DatabaseHelper
            
            // Broadcast the new data to UI if needed
            Intent intent = new Intent("com.example.cardioflow.NEW_MEASUREMENT");
            intent.putExtra("data", json);
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
