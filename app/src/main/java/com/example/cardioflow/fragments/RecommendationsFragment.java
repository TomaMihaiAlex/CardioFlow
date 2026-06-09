package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardioflow.R;
import com.example.cardioflow.adapters.RecommendationAdapter;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.User;

import java.util.List;

public class RecommendationsFragment extends Fragment {

    private RecyclerView rvRecommendations;
    private RecommendationAdapter adapter;

    public RecommendationsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvRecommendations = view.findViewById(R.id.rv_recommendations);
        rvRecommendations.setLayoutManager(new LinearLayoutManager(getContext()));

        User currentUser = AuthManager.getInstance(requireContext()).getCurrentUser();
        if (currentUser != null) {
            List<Recommendation> recommendations = DataManager.getInstance(requireContext())
                    .getRecommendationsForPatient(currentUser.getId());
            adapter = new RecommendationAdapter(recommendations, DatabaseManager.getInstance(requireContext()));
            rvRecommendations.setAdapter(adapter);
        }
    }
}