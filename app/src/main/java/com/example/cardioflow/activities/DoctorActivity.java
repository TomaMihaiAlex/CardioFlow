package com.example.cardioflow.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardioflow.R;

public class DoctorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);
        TextView tv = findViewById(R.id.tv_doctor_welcome);
        tv.setText("Bine ai venit, doctor! Aici vei vedea lista pacienților.");
    }
}