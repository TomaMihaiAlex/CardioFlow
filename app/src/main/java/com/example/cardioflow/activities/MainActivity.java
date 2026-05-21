package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.cardioflow.auth.AuthManager;

import com.example.cardioflow.HistoryFragment;
import com.example.cardioflow.HomeFragment;
import com.example.cardioflow.R;
import com.example.cardioflow.RecommendationsFragment;

public class MainActivity extends AppCompatActivity {

    private Button btnHome, btnRecommendations, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!AuthManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            AuthManager.getInstance(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}