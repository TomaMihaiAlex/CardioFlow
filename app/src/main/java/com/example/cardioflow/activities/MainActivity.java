package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.cardioflow.auth.AuthManager;

import com.example.cardioflow.fragments.HistoryFragment;
import com.example.cardioflow.fragments.HomeFragment;
import com.example.cardioflow.R;
import com.example.cardioflow.fragments.RecommendationsFragment;
import com.example.cardioflow.models.User;

public class MainActivity extends AppCompatActivity {

    private Button btnHome, btnRecommendations, btnHistory, btnAbout, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!AuthManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User currentUser = AuthManager.getInstance(this).getCurrentUser();

        TextView tv = findViewById(R.id.tv_patient_welcome);
        tv.setText("Bine ai venit, pacient " + currentUser.getFirstName() + " " + currentUser.getLastName());

        btnHome = findViewById(R.id.btn_home);
        btnRecommendations = findViewById(R.id.btn_recommendations);
        btnHistory = findViewById(R.id.btn_history);
        btnAbout = findViewById(R.id.btn_about);
        btnLogout = findViewById(R.id.btn_logout);

        // Încarcă fragmentul Home la pornire
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        btnHome.setOnClickListener(v -> {
            Log.d("MainActivity", "Buton Acasă apăsat");
            loadFragment(new HomeFragment());
        });
        btnRecommendations.setOnClickListener(v -> {
            Log.d("MainActivity", "Buton Recomandări apăsat");
            loadFragment(new RecommendationsFragment());
        });
        btnHistory.setOnClickListener(v -> {
            Log.d("MainActivity", "Buton Istoric apăsat");
            loadFragment(new HistoryFragment());
        });
        btnAbout.setOnClickListener(v -> {
            Log.d("MainActivity", "Buton Despre apăsat");
            showAboutDialog();
        });
        btnLogout.setOnClickListener(v -> {
            Log.d("MainActivity", "Buton Deconectare apăsat");
            AuthManager.getInstance(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public void showAboutDialog() {
        String versionName = "1.0"; // poti lua din BuildConfig
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