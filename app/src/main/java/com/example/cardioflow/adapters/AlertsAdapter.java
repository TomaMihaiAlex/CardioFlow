package com.example.cardioflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.net.ParseException;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardioflow.models.Alert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {
    private List<Alert> alerts;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public AlertsAdapter(List<Alert> alerts) { this.alerts = alerts; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert a = alerts.get(position);
        String formattedDate = a.getTimestamp();
        try {
            Date date = inputFormat.parse(a.getTimestamp());
            formattedDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
        holder.text1.setText(String.format("%s: %s (%.0f) - %s", formattedDate, a.getType(), a.getValue(), a.getSeverity()));
        holder.text2.setText("Pacient: " + (a.getUserText().isEmpty() ? "fără comentariu" : a.getUserText()));
    }

    @Override
    public int getItemCount() { return alerts.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View v) { super(v); text1 = v.findViewById(android.R.id.text1); text2 = v.findViewById(android.R.id.text2); }
    }
}