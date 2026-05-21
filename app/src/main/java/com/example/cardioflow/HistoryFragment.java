package com.example.cardioflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {

    public HistoryFragment()
    {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        TextView text = view.findViewById(R.id.text_history);
        text.setText("Istoric măsurători și alarme...");
        return view;
    }
}