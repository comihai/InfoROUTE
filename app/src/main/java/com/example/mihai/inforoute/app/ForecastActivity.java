package com.example.mihai.inforoute.app;


import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;

/**
 * Created by mihai on 5/4/2015.
 */
public class ForecastActivity extends ActionBarActivity {

    private ArrayAdapter<String> mForecastAdapter;
    private String arrivalCity = null, departureCity = null;

    public ForecastActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
        {
            arrivalCity = savedInstanceState.getString("arrivalCity");
            departureCity = savedInstanceState.getString("departureCity");
        }
        setContentView(R.layout.forecast_activity_main);
        Bundle intentBundle = getIntent().getExtras();
        if(intentBundle != null) {
            arrivalCity = intentBundle.getString("arrivalCity");
            departureCity = intentBundle.getString("departureCity");
        }
        Bundle bundle = new Bundle();
        bundle.putString("arrivalCity",arrivalCity);
        bundle.putString("departureCity",departureCity);
        ForecastFragment forecastFragment = new ForecastFragment();
        forecastFragment.setArguments(bundle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, forecastFragment)
                            .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("arrivalCity",arrivalCity);
        outState.putString("departureCity",departureCity);
        super.onSaveInstanceState(outState);
    }


}

