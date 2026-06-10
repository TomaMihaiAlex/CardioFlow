package com.example.cardioflow.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cardioflow.fragments.HistoryFragment;
import com.example.cardioflow.fragments.AlertsFragment;
import com.example.cardioflow.fragments.RecommendationsFragmentDoctor;
import com.example.cardioflow.fragments.ThresholdsConfigFragment;
import com.example.cardioflow.fragments.PatientBodyFragment;
import com.example.cardioflow.fragments.PatientLiveFragment;
import com.example.cardioflow.R;

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
                return PatientLiveFragment.newInstance(patientId);
            case 1:
                return HistoryFragment.newInstance(patientId);
            case 2:
                return PatientBodyFragment.newInstance(patientId);
            case 3:
                return AlertsFragment.newInstance(patientId);
            case 4:
                return RecommendationsFragmentDoctor.newInstance(patientId);
            case 5:
                return ThresholdsConfigFragment.newInstance(patientId);
            default:
                return PatientLiveFragment.newInstance(patientId);
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    public static String getTabTitle(int position, android.content.Context context) {
        switch (position) {
            case 0: return "Live";
            case 1: return context.getString(R.string.tab_history);
            case 2: return context.getString(R.string.tab_body);
            case 3: return context.getString(R.string.tab_alerts);
            case 4: return context.getString(R.string.tab_recommendations);
            case 5: return context.getString(R.string.tab_config);
            default: return "";
        }
    }
}