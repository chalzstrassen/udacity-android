package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        String forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        TextView forecastDetailView = (TextView) rootView.findViewById(R.id.forecast_text);

        forecastDetailView.setTextSize(35);
        forecastDetailView.setText(forecast);
        return rootView;
    }
}
