package com.example.cardioflow.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import com.example.cardioflow.R;
import com.example.cardioflow.utils.ThemeHelper;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.User;
import com.example.cardioflow.utils.AppConstants;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {
    private EditText etBleInterval;
    private Button btnSave, btnClear, btnSimulate, btnLogout, btnChangePassword;
    private MaterialCardView cardPatientSettings;
    private RadioGroup rgTheme;
    private RadioButton rbStandard, rbLight, rbDark;
    private SwitchMaterial switchSimulation;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        cardPatientSettings = findViewById(R.id.card_patient_settings);
        etBleInterval = findViewById(R.id.et_ble_interval);
        btnSave = findViewById(R.id.btn_save_settings);
        btnSimulate = findViewById(R.id.btn_simulate_ble);
        switchSimulation = findViewById(R.id.switch_simulation);
        
        btnChangePassword = findViewById(R.id.btn_change_password_settings);
        btnClear = findViewById(R.id.btn_clear_data);
        btnLogout = findViewById(R.id.btn_logout_settings);

        rgTheme = findViewById(R.id.rg_theme);
        rbStandard = findViewById(R.id.rb_theme_standard);
        rbLight = findViewById(R.id.rb_theme_light);
        rbDark = findViewById(R.id.rb_theme_dark);

        AuthManager authManager = AuthManager.getInstance(this);
        User currentUser = authManager.getCurrentUser();

        if (currentUser != null && AppConstants.ROLE_DOCTOR.equalsIgnoreCase(currentUser.getRole())) {
            cardPatientSettings.setVisibility(View.GONE);
        }

        etBleInterval.setText(String.valueOf(prefs.getInt(AppConstants.KEY_BLE_INTERVAL, 10)));
        switchSimulation.setChecked(prefs.getBoolean(AppConstants.KEY_SIMULATION_MODE, true));

        // Load current theme selection
        int currentTheme = prefs.getInt(AppConstants.KEY_THEME, AppConstants.THEME_STANDARD);
        if (currentTheme == AppConstants.THEME_LIGHT) rbLight.setChecked(true);
        else if (currentTheme == AppConstants.THEME_DARK) rbDark.setChecked(true);
        else rbStandard.setChecked(true);

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedTheme = AppConstants.THEME_STANDARD;
            if (checkedId == R.id.rb_theme_light) selectedTheme = AppConstants.THEME_LIGHT;
            else if (checkedId == R.id.rb_theme_dark) selectedTheme = AppConstants.THEME_DARK;

            prefs.edit().putInt(AppConstants.KEY_THEME, selectedTheme).apply();
            ThemeHelper.applyTheme(this);
        });

        switchSimulation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(AppConstants.KEY_SIMULATION_MODE, isChecked).apply();
            if (!isChecked) {
                // If turning off simulation, user might want to connect to BLE
                Toast.makeText(this, "Simulare dezactivată. Conectați senzorul BLE.", Toast.LENGTH_SHORT).show();
            } else {
                // If turning on simulation, stop BLE service
                Intent stopBle = new Intent(this, com.example.cardioflow.services.BLEReceiverService.class);
                stopService(stopBle);
            }
        });

        btnSave.setOnClickListener(v -> {
            String input = etBleInterval.getText().toString();
            if (!input.isEmpty()) {
                int interval = Integer.parseInt(input);
                prefs.edit().putInt(AppConstants.KEY_BLE_INTERVAL, interval).apply();
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            }
        });

        btnSimulate.setOnClickListener(v -> {
            startActivity(new Intent(this, DeviceScanActivity.class));
        });

        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });

        btnClear.setOnClickListener(v -> {
            DatabaseManager.getInstance(this).clearAllData();
            Toast.makeText(this, R.string.local_data_cleared, Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void applyTheme(int themeValue) {
        ThemeHelper.applyTheme(this);
    }
}
