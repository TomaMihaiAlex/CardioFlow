package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.utils.ThemeHelper;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.adapters.PatientAdapter;
import com.example.cardioflow.fragments.PatientDetailsFragment;
import com.example.cardioflow.models.User;
import java.util.List;

public class DoctorActivity extends AppCompatActivity {
    private Button btnAbout, btnSettings;
    private RecyclerView rvPatients;
    private PatientAdapter adapter;
    private List<User> patientList;
    private FrameLayout detailsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        AuthManager authManager = AuthManager.getInstance(this);
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        rvPatients = findViewById(R.id.rv_patients);
        detailsContainer = findViewById(R.id.details_container);
        btnAbout = findViewById(R.id.btn_doctor_about);
        btnSettings = findViewById(R.id.btn_doctor_settings);

        if (rvPatients == null) {
            Log.e("DoctorActivity", "RecyclerView rv_patients not found!");
        } else {
            rvPatients.setLayoutManager(new LinearLayoutManager(this));
        }

        User currentDoctor = authManager.getCurrentUser();
        if (currentDoctor == null) {
            Log.e("DoctorActivity", "Current doctor is null!");
            finish();
            return;
        }
        String doctorId = currentDoctor.getId(); 
        patientList = authManager.getPatientsForDoctor(doctorId);

        adapter = new PatientAdapter(patientList, patient -> {
            if (detailsContainer != null) {
                detailsContainer.setVisibility(View.VISIBLE);
            }
            PatientDetailsFragment fragment = PatientDetailsFragment.newInstance(patient.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_container, fragment)
                    .commit();
        });

        if (rvPatients != null) {
            rvPatients.setAdapter(adapter);
        }

        TextView tv = findViewById(R.id.tv_doctor_welcome);
        if (tv != null) {
            tv.setText(getString(R.string.welcome_doctor, currentDoctor.getFirstName(), currentDoctor.getLastName()));
        }

        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                Log.d("DoctorActivity", "Buton Despre apăsat");
                showAboutDialog();
            });
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }
    }

    public void showAboutDialog() {
        String versionName = "1.0";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) { }

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(getString(R.string.about_message, versionName))
                .setPositiveButton(R.string.btn_confirm, null)
                .show();
    }
}