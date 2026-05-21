package com.example.cardioflow.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.models.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerRole;
    private Button btnRegister;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance(this);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerRole = findViewById(R.id.spinner_role);
        btnRegister = findViewById(R.id.btn_register);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completați toate câmpurile", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Parolele nu coincid", Toast.LENGTH_SHORT).show();
                return;
            }
            User newUser = new User(null, email, password, role, firstName, lastName, "");
            if (authManager.register(newUser)) {
                Toast.makeText(this, "Cont creat cu succes! Vă puteți autentifica.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Email deja existent", Toast.LENGTH_SHORT).show();
            }
        });
    }
}