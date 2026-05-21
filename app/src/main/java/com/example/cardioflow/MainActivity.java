package com.example.cardioflow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private Button btnHome, btnRecommendations, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnHome = findViewById(R.id.btn_home);
        btnRecommendations = findViewById(R.id.btn_recommendations);
        btnHistory = findViewById(R.id.btn_history);

        // Încarcă fragmentul Home la pornire
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        btnHome.setOnClickListener(v -> loadFragment(new HomeFragment()));
        btnRecommendations.setOnClickListener(v -> loadFragment(new RecommendationsFragment()));
        btnHistory.setOnClickListener(v -> loadFragment(new HistoryFragment()));
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}