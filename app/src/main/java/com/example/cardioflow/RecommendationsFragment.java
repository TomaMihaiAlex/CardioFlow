package com.example.cardioflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class RecommendationsFragment extends Fragment {

    public RecommendationsFragment()
    {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendations, container, false);
        TextView text = view.findViewById(R.id.text_recommendations);
        text.setText("Recomandări: bicicletă 30 min/zi, etc.");
        return view;
    }
}