package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.User;
import com.example.cardioflow.auth.AuthManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecommendationsFragmentDoctor extends Fragment {
    private String patientId;
    private RecyclerView recyclerView;
    private EditText etType, etDuration, etInstructions;
    private Button btnAdd;
    private List<Recommendation> recommendationsList = new ArrayList<>();
    private RecommendationsAdapter adapter;

    public static RecommendationsFragmentDoctor newInstance(String patientId) {
        RecommendationsFragmentDoctor fragment = new RecommendationsFragmentDoctor();
        Bundle args = new Bundle();
        args.putString("patientId", patientId);
        fragment.setArguments(args);
        return fragment;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendations_doctor, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        etType = view.findViewById(R.id.et_type);
        etDuration = view.findViewById(R.id.et_duration);
        etInstructions = view.findViewById(R.id.et_instructions);
        btnAdd = view.findViewById(R.id.btn_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecommendationsAdapter(recommendationsList);
        recyclerView.setAdapter(adapter);

        loadRecommendations();
        btnAdd.setOnClickListener(v -> addRecommendation());
        return view;
    }

    private void loadRecommendations() {
        FirebaseManager.getInstance().listenForRecommendations(patientId, recommendations -> {
            if (recommendations != null) {
                recommendationsList.clear();
                recommendationsList.addAll(recommendations);
                adapter.notifyDataSetChanged();
                
                // Sync to local SQLite as well
                DatabaseManager db = DatabaseManager.getInstance(requireContext());
                for (Recommendation r : recommendations) {
                    db.insertRecommendation(r);
                }
            }
        });
    }

    private void addRecommendation() {
        String type = etType.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        
        if (type.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(getContext(), "Completați toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int duration = Integer.parseInt(durationStr);
        User doctor = AuthManager.getInstance(requireContext()).getCurrentUser();
        String doctorId = doctor != null ? doctor.getId() : "unknown";
        
        Recommendation rec = new Recommendation(
                UUID.randomUUID().toString(), 
                patientId, 
                doctorId, 
                type, 
                duration, 
                instructions, 
                "medium"
        );
        
        // Save to Local SQLite
        DatabaseManager.getInstance(requireContext()).insertRecommendation(rec);
        
        // Save to Firestore
        FirebaseManager.getInstance().saveRecommendation(rec);

        Toast.makeText(getContext(), "Recomandare salvată!", Toast.LENGTH_SHORT).show();
        etType.setText("");
        etDuration.setText("");
        etInstructions.setText("");
    }

    class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {
        private List<Recommendation> list;
        RecommendationsAdapter(List<Recommendation> list) { this.list = list; }
        
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation_doctor, parent, false);
            return new ViewHolder(v);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Recommendation r = list.get(position);
            holder.tvType.setText(r.getType());
            holder.tvDuration.setText(r.getDailyDurationMin() + " min/zi");
            holder.tvInstructions.setText(r.getInstructions());
            
            holder.btnDelete.setOnClickListener(v -> {
                DatabaseManager.getInstance(requireContext()).deleteRecommendation(r.getRecommendationId());
                FirebaseManager.getInstance().deleteRecommendation(r.getRecommendationId());
            });
        }
        
        @Override
        public int getItemCount() { return list.size(); }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvType, tvInstructions, tvDuration;
            View btnDelete;
            ViewHolder(View v) { 
                super(v); 
                tvType = v.findViewById(R.id.tv_rec_type); 
                tvInstructions = v.findViewById(R.id.tv_rec_instructions);
                tvDuration = v.findViewById(R.id.tv_rec_duration);
                btnDelete = v.findViewById(R.id.btn_delete_rec);
            }
        }
    }
}
