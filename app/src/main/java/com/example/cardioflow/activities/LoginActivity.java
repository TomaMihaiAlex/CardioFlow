package com.example.cardioflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnForgotPassword;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            navigateBasedOnRole();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completați toate câmpurile", Toast.LENGTH_SHORT).show();
                return;
            }
            if (authManager.login(email, password)) {
                Toast.makeText(this, "Autentificare reușită", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole();
            } else {
                Toast.makeText(this, "Email sau parolă incorecte", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
    }

    private void navigateBasedOnRole() {
        String role = authManager.getCurrentUser().getRole();
        if (role.equals("medic")) {
            startActivity(new Intent(this, DoctorActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}