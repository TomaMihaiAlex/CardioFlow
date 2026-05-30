package com.example.cardioflow.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cardioflow.fragments.HistoryFragment;
import com.example.cardioflow.fragments.AlertsFragment;
import com.example.cardioflow.fragments.RecommendationsFragmentDoctor;
import com.example.cardioflow.fragments.ThresholdsConfigFragment;

public class PatientDetailsPagerAdapter extends FragmentStateAdapter {

    private final String patientId;

    public PatientDetailsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String patientId) {
        super(fragmentActivity);
        this.patientId = patientId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return HistoryFragment.newInstance(patientId);
            case 1:
                return AlertsFragment.newInstance(patientId);
            case 2:
                return RecommendationsFragmentDoctor.newInstance(patientId);
            case 3:
                return ThresholdsConfigFragment.newInstance(patientId);
            default:
                return HistoryFragment.newInstance(patientId);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    public static String getTabTitle(int position) {
        switch (position) {
            case 0: return "Istoric";
            case 1: return "Alarme";
            case 2: return "Recomandări";
            case 3: return "Configurări";
            default: return "";
        }
    }
}