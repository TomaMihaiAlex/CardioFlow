package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.models.User;
import com.example.cardioflow.utils.AppConstants;
import com.example.cardioflow.utils.ThemeHelper;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            directLogin();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        
        findViewById(R.id.btn_login).setOnClickListener(v -> performLogin());
        findViewById(R.id.btn_register).setOnClickListener(v -> 
            startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btn_forgot_password).setOnClickListener(v -> 
            startActivity(new Intent(this, ChangePasswordActivity.class)));
    }

    private void performLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_fill_all_fields));
            return;
        }
        if (pass.isEmpty()) {
            tilPassword.setError(getString(R.string.error_fill_all_fields));
            return;
        }

        authManager.loginFirebase(email, pass, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                directLogin();
            }

            @Override
            public void onError(String error) {
                tilPassword.setError(error);
            }
        });
    }

    private void directLogin() {
        String role = authManager.getCurrentUser().getRole();
        Intent intent = "medic".equalsIgnoreCase(role) || AppConstants.ROLE_DOCTOR.equalsIgnoreCase(role) ? 
            new Intent(this, DoctorActivity.class) : 
            new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
