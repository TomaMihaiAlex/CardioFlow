package com.example.cardioflow.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Recommendation;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {
    private List<Recommendation> recommendations;
    private DatabaseManager dbManager;
    private String today;

    public RecommendationAdapter(List<Recommendation> recommendations, DatabaseManager dbManager) {
        this.recommendations = recommendations;
        this.dbManager = dbManager;
        this.today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommendation r = recommendations.get(position);
        holder.tvType.setText(r.getType());
        holder.tvInstructions.setText(r.getInstructions());
        holder.tvDuration.setText(r.getDailyDurationMin() + " min/zi");

        boolean isDone = dbManager.isRecommendationDone(r.getRecommendationId(), today);
        holder.cbDone.setChecked(isDone);
        updateUI(holder, isDone);

        holder.cbDone.setOnClickListener(v -> {
            boolean checked = holder.cbDone.isChecked();
            dbManager.setRecommendationDone(r.getRecommendationId(), today, checked);
            updateUI(holder, checked);
        });
    }

    private void updateUI(ViewHolder holder, boolean isDone) {
        if (isDone) {
            holder.tvType.setPaintFlags(holder.tvType.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvType.setTextColor(Color.GRAY);
        } else {
            holder.tvType.setPaintFlags(holder.tvType.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvType.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() { return recommendations.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvInstructions, tvDuration;
        CheckBox cbDone;
        ViewHolder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tv_rec_type);
            tvInstructions = v.findViewById(R.id.tv_rec_instructions);
            tvDuration = v.findViewById(R.id.tv_rec_duration);
            cbDone = v.findViewById(R.id.cb_rec_done);
        }
    }
}