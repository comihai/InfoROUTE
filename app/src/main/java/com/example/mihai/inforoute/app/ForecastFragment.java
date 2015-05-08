/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mihai.inforoute.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.widget.ListView} layout.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    private String arrivalCity=null, departureCity = null;
    private TextView text_dist, text_status, text_speed, text_time, text_cons, text_totalCons, text_cost, text_index;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();

        if(bundle != null) {
            arrivalCity = bundle.getString("arrivalCity");
            departureCity = bundle.getString("departureCity");
            getActivity().setTitle(departureCity + " >> "+arrivalCity);
        }
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }
    private void updatePage()
    {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String noDays = prefs.getString(getString(R.string.pref_noDays_key),
                getString(R.string.pref_noDays_default));
        weatherTask.execute(arrivalCity, noDays);

        FetchRouteTask routeTask = new FetchRouteTask();
        routeTask.execute(departureCity,arrivalCity);
    }

    @Override
    public void onStart() {
        super.onStart();
        updatePage();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        text_dist = (TextView)rootView.findViewById(R.id.list_item_distance_textview);
        text_dist.setText("");
        text_dist.setVisibility(View.GONE);
        text_status = (TextView)rootView.findViewById(R.id.list_item_status_textview);
        text_status.setText("");
        text_status.setVisibility(View.GONE);

        text_speed = (TextView)rootView.findViewById(R.id.list_item_speed_textview);
        text_speed.setText("");
        text_speed.setVisibility(View.GONE);
        text_time = (TextView)rootView.findViewById(R.id.list_item_time_textview);
        text_time.setText("");
        text_time.setVisibility(View.GONE);
        text_cons = (TextView)rootView.findViewById(R.id.list_item_consum_textview);
        text_cons.setText("");
        text_cons.setVisibility(View.GONE);
        text_totalCons = (TextView)rootView.findViewById(R.id.list_item_consum_total_textview);
        text_totalCons.setText("");
        text_totalCons.setVisibility(View.GONE);
        text_cost = (TextView)rootView.findViewById(R.id.list_item_cost_textview);
        text_cost.setText("");
        text_cost.setVisibility(View.GONE);
        text_index = (TextView)rootView.findViewById(R.id.list_item_index_textview);
        text_index.setText("");
        text_index.setVisibility(View.GONE);
        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            return resultStrs;

        }
        @Override
        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, params[1])
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Jsonul este  : "+forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(params[1]));
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for(String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
    public class FetchRouteTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchRouteTask.class.getSimpleName();
        private String formatSpeed(String speed, String unitType)
        {
            int speedInt = Integer.parseInt(speed);
            String r = null;
            if(unitType.equals(getString(R.string.pref_speedUnits_m_s)))
            {
                speedInt = speedInt * 10/36;
                r = Integer.toString(speedInt) + " m/s";
            }
            else
            {
                r = Integer.toString(speedInt) + " Km/h";
            }
            return r;
        }
        private String formatTime(String time)
        {
            String[] parts = time.split("\\.");
            String part1 = parts[0];
            String part2 = parts[1];
            String r = part1 + " h " + part2 + " min";
            return r;
        }

        private String[] getRouteDataFromJson(String routeJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_DISTANTA = "distanta";
            final String OWM_STATUS = "status";
            final String OWM_VITEZA = "viteza";
            final String OWM_TIMP = "timp";
            final String OWM_CONSUM = "consum";
            final String OWM_CONSUM_TOTAL = "consum_total";
            final String OWM_COST = "cost";
            final String OWM_INDICE = "indice";

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String speed = sharedPreferences.getString(getString(R.string.pref_speed_key),getString(R.string.pref_speedUnits_km_h));

                    JSONObject routeJson = new JSONObject(routeJsonStr);
            String resultStrs[] = new String[8];

            resultStrs[0] = routeJson.getString(OWM_DISTANTA) + " Km";
            resultStrs[1] = routeJson.getString(OWM_STATUS);
            resultStrs[2] = formatSpeed(routeJson.getString(OWM_VITEZA),speed);
            resultStrs[3] = formatTime(routeJson.getString(OWM_TIMP));
            resultStrs[4] = routeJson.getString(OWM_CONSUM) + " l/Km";
            resultStrs[5] = routeJson.getString(OWM_CONSUM_TOTAL) + " l";
            resultStrs[6] = routeJson.getString(OWM_COST) + " RON";
            resultStrs[7] = routeJson.getString(OWM_INDICE);
            return resultStrs;
        }
        @Override
        protected String[] doInBackground(String... params) {
            Log.v(LOG_TAG, "Params  : "+ params[0] + " "+params[1]);
            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String routeJsonStr = null;


            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String ROUTE_BASE_URL =
                        "http://192.168.0.106:8080/routeInfo/index.php?";
                final String NAME1_PARAM = "name1";
                final String NAME2_PARAM = "name2";


                Uri builtUri = Uri.parse(ROUTE_BASE_URL).buildUpon()
                        .appendQueryParameter(NAME1_PARAM, params[0])
                        .appendQueryParameter(NAME2_PARAM, params[1])
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Url  : "+url.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                routeJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Jsonul route este  : "+routeJsonStr);
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }
            finally
            {
                if (urlConnection != null)
                {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try
            {
                return getRouteDataFromJson(routeJsonStr);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                text_dist.setText(result[0]);
                text_dist.setVisibility(View.VISIBLE);

                text_status.setText(result[1]);
                text_status.setVisibility(View.VISIBLE);

                text_speed.setText(result[2]);
                text_speed.setVisibility(View.VISIBLE);

                text_time.setText(result[3]);
                text_time.setVisibility(View.VISIBLE);

                text_cons.setText(result[4]);
                text_cons.setVisibility(View.VISIBLE);

                text_totalCons.setText(result[5]);
                text_totalCons.setVisibility(View.VISIBLE);

                text_cost.setText(result[6]);
                text_cost.setVisibility(View.VISIBLE);

                text_index.setText(result[7]);
                text_index.setVisibility(View.VISIBLE);
            }
        }
    }
}
