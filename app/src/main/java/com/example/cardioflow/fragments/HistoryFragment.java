package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private Spinner spinnerFilter;
    private String patientId;

    public static HistoryFragment newInstance(String patientId) {
        HistoryFragment fragment = new HistoryFragment();
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
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        spinnerFilter = view.findViewById(R.id.spinner_filter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        initFilter();
        return view;
    }

    private void initFilter() {
        String[] options = {
                getString(R.string.filter_all),
                getString(R.string.filter_hr),
                getString(R.string.filter_spo2),
                getString(R.string.filter_temp),
                getString(R.string.filter_hum),
                getString(R.string.filter_alarms)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadHistory(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadHistory(int type) {
        DatabaseManager db = DatabaseManager.getInstance(requireContext());
        List<Object> data = new ArrayList<>();
        
        if (type == 5 || type == 0) {
            data.addAll(db.getAllAlerts());
        }
        
        if (type != 5) {
            List<Measurement> measurements = db.getLastReadings(100);
            for (Measurement m : measurements) {
                if (type == 0 || type == 1 || type == 2 || type == 3 || type == 4) {
                    data.add(m);
                }
            }
        }
        recyclerView.setAdapter(new HistoryAdapter(data));
    }

    class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Object> list;

        HistoryAdapter(List<Object> list) { this.list = list; }

        @Override
        public int getItemViewType(int position) {
            return (list.get(position) instanceof Alert) ? 1 : 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            HistoryViewHolder vh = (HistoryViewHolder) holder;
            Object obj = list.get(position);
            if (obj instanceof Alert) {
                Alert a = (Alert) obj;
                vh.title.setText(getString(R.string.history_alarm_format, a.getType(), a.getValue()));
                vh.title.setTextColor(android.graphics.Color.RED);
                vh.subtitle.setText(a.getTimestamp() + " - " + (a.getUserText() != null ? a.getUserText() : getString(R.string.history_no_comment)));
            } else {
                Measurement m = (Measurement) obj;
                vh.title.setText(getString(R.string.history_measurement_hr_spo2, m.getHeartRate(), m.getSpo2()));
                vh.title.setTextColor(requireContext().getResources().getColor(android.R.color.black, null));
                vh.subtitle.setText(getString(R.string.history_measurement_temp_hum_time, m.getTemperature(), m.getHumidity(), m.getTimestamp()));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            HistoryViewHolder(View v) { 
                super(v); 
                title = v.findViewById(R.id.tv_title); 
                subtitle = v.findViewById(R.id.tv_subtitle); 
            }
        }
    }
}
