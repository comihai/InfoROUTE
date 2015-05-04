package com.example.mihai.inforoute.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;

/**
 * Created by mihai on 5/4/2015.
 */
public class ForecastActivity extends ActionBarActivity {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forecast_activity_main);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString("arrivalCity",message);
        ForecastFragment forecastFragment = new ForecastFragment();
        forecastFragment.setArguments(bundle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, forecastFragment)
                            .commit();
        }
    }
}

