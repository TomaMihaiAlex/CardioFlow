package com.example.cardioflow.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardioflow.R;
import com.example.cardioflow.services.BLEReceiverService;
import com.example.cardioflow.utils.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DeviceAdapter adapter;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();

    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth dezactivat!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        RecyclerView rv = findViewById(R.id.rv_devices);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter();
        rv.setAdapter(adapter);

        Button btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());

        checkPermissionsAndScan();
    }

    private void checkPermissionsAndScan() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        } else {
            scanLeDevice();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanLeDevice();
            } else {
                Toast.makeText(this, "Permisiuni necesare pentru scanare", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {
            deviceList.clear();
            adapter.notifyDataSetChanged();
            
            handler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                findViewById(R.id.btn_scan).setEnabled(true);
                Toast.makeText(this, "Scanare finalizată", Toast.LENGTH_SHORT).show();
            }, SCAN_PERIOD);

            scanning = true;
            findViewById(R.id.btn_scan).setEnabled(false);
            bluetoothLeScanner.startScan(leScanCallback);
            Toast.makeText(this, "Se caută dispozitive...", Toast.LENGTH_SHORT).show();
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            runOnUiThread(() -> {
                if (!deviceList.contains(device)) {
                    deviceList.add(device);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };

    class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice device = deviceList.get(position);
            String name = device.getName();
            holder.text1.setText(name != null ? name : "Dispozitiv Necunoscut");
            holder.text2.setText(device.getAddress());
            
            holder.itemView.setOnClickListener(v -> {
                if (scanning) {
                    bluetoothLeScanner.stopScan(leScanCallback);
                    scanning = false;
                }
                
                SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(AppConstants.KEY_LAST_MAC, device.getAddress()).apply();
                prefs.edit().putBoolean(AppConstants.KEY_SIMULATION_MODE, false).apply();

                Intent intent = new Intent(DeviceScanActivity.this, BLEReceiverService.class);
                intent.putExtra("device_address", device.getAddress());
                startForegroundService(intent);
                
                Toast.makeText(DeviceScanActivity.this, "Conectare la " + (name != null ? name : device.getAddress()), Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        @Override
        public int getItemCount() { return deviceList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
