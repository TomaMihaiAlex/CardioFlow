package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.cardioflow.R;
import com.example.cardioflow.adapters.PatientDetailsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PatientDetailsFragment extends Fragment {
    private String patientId;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    public static PatientDetailsFragment newInstance(String patientId) {
        PatientDetailsFragment f = new PatientDetailsFragment();
        Bundle args = new Bundle();
        args.putString("patientId", patientId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString("patientId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_details, container, false);
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);

        // Folosește requireActivity() pentru a obține FragmentActivity
        PatientDetailsPagerAdapter adapter = new PatientDetailsPagerAdapter(requireActivity(), patientId);
        viewPager.setAdapter(adapter);

        // Leagă TabLayout de ViewPager2 folosind TabLayoutMediator
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(PatientDetailsPagerAdapter.getTabTitle(position, requireContext()))
        ).attach();

        return view;
    }
}