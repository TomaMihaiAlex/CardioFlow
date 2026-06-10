package com.example.cardioflow.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.models.User;
import com.example.cardioflow.utils.ThemeHelper;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword;
    private Spinner spinnerRole;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance(this);
        initViews();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        findViewById(R.id.btn_register).setOnClickListener(v -> handleRegistration());
    }

    private void initViews() {
        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerRole = findViewById(R.id.spinner_role);
    }

    private void handleRegistration() {
        clearErrors();
        
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String conf = etConfirmPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        boolean valid = true;

        if (fName.isEmpty()) {
            tilFirstName.setError(getString(R.string.error_fill_all_fields));
            valid = false;
        }
        if (lName.isEmpty()) {
            tilLastName.setError(getString(R.string.error_fill_all_fields));
            valid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_fill_all_fields));
            valid = false;
        }
        if (pass.isEmpty()) {
            tilPassword.setError(getString(R.string.error_fill_all_fields));
            valid = false;
        }
        if (!pass.equals(conf)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            valid = false;
        }

        if (!valid) return;

        User user = new User(null, email, pass, role, fName, lName, "");
        
        authManager.registerFirebase(user, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User registeredUser) {
                Toast.makeText(RegisterActivity.this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegisterActivity.this, "Eroare Cloud: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearErrors() {
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}
