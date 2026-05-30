package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.cardioflow.R;

public class RecommendationsFragment extends Fragment {

    public RecommendationsFragment()
    {}

    public static RecommendationsFragmentDoctor newInstance(String patientId) {
        RecommendationsFragmentDoctor fragment = new RecommendationsFragmentDoctor();
        Bundle args = new Bundle();
        args.putString("patientId", patientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendations, container, false);
        TextView text = view.findViewById(R.id.text_recommendations);
        text.setText("Recomandări: bicicletă 30 min/zi, etc.");
        return view;
    }
}