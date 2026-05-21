package com.example.cardioflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvLastHr, tvLastSpo2, tvLastTemp, tvLastHum;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflatează noul layout (fragment_home.xml)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inițializează TextView-urile din card
        tvLastHr = view.findViewById(R.id.tv_last_hr);
        tvLastSpo2 = view.findViewById(R.id.tv_last_spo2);
        tvLastTemp = view.findViewById(R.id.tv_last_temp);
        tvLastHum = view.findViewById(R.id.tv_last_hum);

        // Valori simulate (după care le vei înlocui cu date reale)
        updateLastValues(75, 98, 36.6, 42.5);
    }

    // Metode apelate de atributele android:onClick din layout
    public void onHeartClick(View view) {
        Toast.makeText(getContext(), "❤️ Ritm cardiac: 75 bpm, ECG: normal", Toast.LENGTH_LONG).show();
    }

    public void onLungsClick(View view) {
        Toast.makeText(getContext(), "🫁 Saturație oxigen: 98%", Toast.LENGTH_LONG).show();
    }

    public void onSkinClick(View view) {
        Toast.makeText(getContext(), "🌡️ Temperatură: 36.6°C, Umiditate: 42.5%", Toast.LENGTH_LONG).show();
    }

    private void updateLastValues(int hr, int spo2, double temp, double hum) {
        tvLastHr.setText("Puls: " + hr + " bpm");
        tvLastSpo2.setText("SpO₂: " + spo2 + " %");
        tvLastTemp.setText("Temperatură: " + temp + " °C");
        tvLastHum.setText("Umiditate: " + hum + " %");
    }
}