package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cardioflow.R;
import com.example.cardioflow.utils.ThemeHelper;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.fragments.HistoryFragment;
import com.example.cardioflow.fragments.HomeFragment;
import com.example.cardioflow.fragments.RecommendationsFragment;
import com.example.cardioflow.models.User;
import com.example.cardioflow.database.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        FirebaseManager.initializeManual(this);

        if (!AuthManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User user = AuthManager.getInstance(this).getCurrentUser();
        if (user != null) {
            TextView tv = findViewById(R.id.tv_patient_welcome);
            tv.setText(getString(R.string.welcome_patient, user.getFirstName(), user.getLastName()));
        }

        initNavigation();
        
        findViewById(R.id.btn_about).setOnClickListener(v -> showAbout());
        findViewById(R.id.btn_settings).setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void initNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            load(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) load(new HomeFragment());
            else if (id == R.id.nav_recommendations) load(new RecommendationsFragment());
            else if (id == R.id.nav_history) load(new HistoryFragment());
            return true;
        });
    }

    private void load(Fragment f) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null && current.getClass().equals(f.getClass())) return;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }

    private void showAbout() {
        String ver = "1.0";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) { }

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(getString(R.string.about_message, ver))
                .setPositiveButton(R.string.btn_confirm, null)
                .show();
    }
}
