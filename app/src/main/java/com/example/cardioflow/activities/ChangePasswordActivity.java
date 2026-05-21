package com.example.cardioflow.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;

public class ChangePasswordActivity extends AppCompatActivity {
    private EditText etEmail, etOldPassword, etNewPassword, etConfirmPassword;
    private Button btnChange;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        authManager = AuthManager.getInstance(this);

        etEmail = findViewById(R.id.et_email);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnChange = findViewById(R.id.btn_change);

        btnChange.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Completați toate câmpurile", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Parolele noi nu coincid", Toast.LENGTH_SHORT).show();
                return;
            }
            if (authManager.changePassword(email, oldPass, newPass)) {
                Toast.makeText(this, "Parolă schimbată cu succes", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Email sau parolă veche incorectă", Toast.LENGTH_SHORT).show();
            }
        });
    }
}