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
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.User;
import com.example.cardioflow.auth.AuthManager;
import java.util.List;
import java.util.UUID;

public class RecommendationsFragmentDoctor extends Fragment {
    private String patientId;
    private RecyclerView recyclerView;
    private EditText etType, etDuration, etInstructions;
    private Button btnAdd;

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
        loadRecommendations();

        btnAdd.setOnClickListener(v -> addRecommendation());
        return view;
    }

    private void loadRecommendations() {
        List<Recommendation> recs = DataManager.getInstance(requireContext()).getRecommendationsForPatient(patientId);
        recyclerView.setAdapter(new RecommendationsAdapter(recs));
    }

    private void addRecommendation() {
        String type = etType.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        if (type.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(getContext(), "Completați tipul și durata", Toast.LENGTH_SHORT).show();
            return;
        }
        int duration = Integer.parseInt(durationStr);
        User doctor = AuthManager.getInstance(requireContext()).getCurrentUser();
        Recommendation rec = new Recommendation(UUID.randomUUID().toString(), patientId, doctor.getId(), type, duration, instructions, "medium");
        DataManager.getInstance(requireContext()).addRecommendation(rec); // necesită implementare
        Toast.makeText(getContext(), "Recomandare adăugată", Toast.LENGTH_SHORT).show();
        etType.setText("");
        etDuration.setText("");
        etInstructions.setText("");
        loadRecommendations();
    }

    class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {
        private List<Recommendation> list;
        RecommendationsAdapter(List<Recommendation> list) { this.list = list; }
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Recommendation r = list.get(position);
            holder.text1.setText(String.format("%s - %d min/zi", r.getType(), r.getDailyDurationMin()));
            holder.text2.setText(r.getInstructions().isEmpty() ? "Fără instrucțiuni" : r.getInstructions());
        }
        @Override
        public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) { super(v); text1 = v.findViewById(android.R.id.text1); text2 = v.findViewById(android.R.id.text2); }
        }
    }
}