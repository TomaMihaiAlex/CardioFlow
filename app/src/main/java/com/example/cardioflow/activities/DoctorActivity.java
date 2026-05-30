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
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.adapters.PatientAdapter;
import com.example.cardioflow.fragments.PatientDetailsFragment;
import com.example.cardioflow.models.User;
import java.util.List;

public class DoctorActivity extends AppCompatActivity {
    private Button btnAbout, btnLogout;
    private RecyclerView rvPatients;
    private PatientAdapter adapter;
    private List<User> patientList;
    private FrameLayout detailsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AuthManager authManager = AuthManager.getInstance(this);
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        User currentDoctor = authManager.getCurrentUser();
        String doctorId = currentDoctor.getId(); // asigură-te că există getId() în User
        patientList = authManager.getPatientsForDoctor(doctorId);

        rvPatients = findViewById(R.id.rv_patients);
        detailsContainer = findViewById(R.id.details_container);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(patientList, patient -> {
            // Setează containerul vizibil înainte de a încărca fragmentul
            detailsContainer.setVisibility(View.VISIBLE);

            PatientDetailsFragment fragment = PatientDetailsFragment.newInstance(patient.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_container, fragment)
                    .commit();
        });
        rvPatients.setAdapter(adapter);

        TextView tv = findViewById(R.id.tv_doctor_welcome);
        tv.setText("Bine ai venit, doctor " + currentDoctor.getFirstName() + " " + currentDoctor.getLastName() + "! Aici vei vedea lista pacienților.");

        btnAbout = findViewById(R.id.btn_about);
        btnLogout = findViewById(R.id.btn_logout);

        btnAbout.setOnClickListener(v -> {
            Log.d("DoctorActivity", "Buton Despre apăsat");
            showAboutDialog();
        });
        btnLogout.setOnClickListener(v -> {
            Log.d("DoctorActivity", "Buton Deconectare apăsat");
            authManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    public void showAboutDialog() {
        String versionName = "1.0";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) { }

        String message = "CardioFlow - Sistem purtabil de supraveghere a stării de sănătate\n\n" +
                "Versiune: " + versionName + "\n" +
                "© 2026 Grupa 6, Echipa 2\n\n" +
                "Programator șef: Toma D. Mihai-Alex\n" +
                "Adjunct: Todică O. Ovidiu-Victor-Nicușor\n" +
                "Secretar: Stan D. Alexandru-Daniel\n\n" +
                "Echipe:\n" +
                "• Web: Sichigea M. Marius-Claudiu, Stănescu E. A. Vlad\n" +
                "• Cloud: Tivig G. Ion-Damian, Șandru P. Petru-Alexandru\n" +
                "• Embedded: Țivlică G. Paul-Matei, Stavenschi Maxim, Todică O. Ovidiu-Victor-Nicușor\n" +
                "• Mobile: Stan D. Alexandru-Daniel, Toma D. Mihai-Alex, Tătaru F. Vlad-Mihai\n\n" +
                "Acest sistem nu înlocuiește un diagnostic medical.\n" +
                "Datele sunt preluate de la senzori și sunt doar orientative.\n\n" +
                "Pentru detalii, consultați documentația tehnică.";

        new AlertDialog.Builder(this)
                .setTitle("Despre CardioFlow")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}