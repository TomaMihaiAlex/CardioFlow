package com.example.cardioflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.models.User;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<User> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(User patient);
    }

    public PatientAdapter(List<User> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        User patient = patients.get(position);
        holder.tvName.setText(patient.getFirstName() + " " + patient.getLastName());
        holder.tvEmail.setText(patient.getEmail());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPatientClick(patient);
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_patient_name);
            tvEmail = itemView.findViewById(R.id.tv_patient_email);
        }
    }
}