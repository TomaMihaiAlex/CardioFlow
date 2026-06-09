package com.example.cardioflow.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.cardioflow.R;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.models.Thresholds;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class BodyPartDialog {

    public enum PartType { HEART, LUNGS, SKIN }

    public static void show(Context context, PartType type, Thresholds thresholds) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_body_part, null);
        dialog.setContentView(view);

        // Setăm lățimea dialogului la 95% din lățimea ecranului
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvCurrentValue = view.findViewById(R.id.tv_current_value);
        TextView tvNormalRange = view.findViewById(R.id.tv_normal_range);
        LineChart chart = view.findViewById(R.id.dialog_chart);
        Button btnClose = view.findViewById(R.id.btn_close_dialog);

        btnClose.setText(context.getString(R.string.btn_close));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        List<Measurement> readings = DatabaseManager.getInstance(context).getLastReadings(10);
        if (readings.isEmpty()) {
            tvCurrentValue.setText(context.getString(R.string.no_data_available));
            dialog.show();
            return;
        }

        Measurement latest = readings.get(0);
        List<Entry> entries = new ArrayList<>();
        String label = "";
        int color = Color.RED;

        switch (type) {
            case HEART:
                tvTitle.setText(context.getString(R.string.body_part_heart));
                tvCurrentValue.setText(context.getString(R.string.current_hr_format, latest.getHeartRate()));
                if (thresholds != null) {
                    tvNormalRange.setText(context.getString(R.string.normal_range_hr, thresholds.getHrMin(), thresholds.getHrMax()));
                }
                for (int i = 0; i < readings.size(); i++) {
                    entries.add(new Entry(i, (float) readings.get(readings.size() - 1 - i).getHeartRate()));
                }
                label = context.getString(R.string.desc_heart);
                color = Color.RED;
                break;
            case LUNGS:
                tvTitle.setText(context.getString(R.string.body_part_lungs));
                tvCurrentValue.setText(context.getString(R.string.current_spo2_format, latest.getSpo2()));
                if (thresholds != null) {
                    tvNormalRange.setText(context.getString(R.string.normal_range_spo2, thresholds.getSpo2Min()));
                }
                for (int i = 0; i < readings.size(); i++) {
                    entries.add(new Entry(i, (float) readings.get(readings.size() - 1 - i).getSpo2()));
                }
                label = context.getString(R.string.desc_lungs);
                color = Color.BLUE;
                break;
            case SKIN:
                tvTitle.setText(context.getString(R.string.body_part_skin));
                tvCurrentValue.setText(context.getString(R.string.current_temp_hum_format, latest.getTemperature(), latest.getHumidity()));
                if (thresholds != null) {
                    tvNormalRange.setText(context.getString(R.string.normal_range_temp, thresholds.getTempMin(), thresholds.getTempMax()));
                }
                for (int i = 0; i < readings.size(); i++) {
                    entries.add(new Entry(i, (float) readings.get(readings.size() - 1 - i).getTemperature()));
                }
                label = context.getString(R.string.desc_skin);
                color = Color.YELLOW;
                break;
        }

        setupChart(chart, entries, label, color);
        dialog.show();
    }

    private static void setupChart(LineChart chart, List<Entry> entries, String label, int color) {
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }
}