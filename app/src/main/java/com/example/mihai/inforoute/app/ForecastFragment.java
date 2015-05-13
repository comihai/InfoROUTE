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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mihai.inforoute.app.adapters.ForecastAdapter;
import com.example.mihai.inforoute.app.adapters.RouteAdapter;
import com.example.mihai.inforoute.app.data.RouteContract;

import java.text.SimpleDateFormat;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.widget.ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private ForecastAdapter mForecastAdapter;
    private RouteAdapter mRouteAdapter;
    private static final int FORECAST_LOADER = 0, ROUTE_LOADER = 1;
    private String arrivalCity=null, departureCity = null;
    private TextView text_dist, text_status, text_speed, text_time, text_cons, text_totalCons, text_cost, text_index;
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            RouteContract.WeatherEntry.TABLE_NAME + "." + RouteContract.WeatherEntry._ID,
            RouteContract.WeatherEntry.COLUMN_DATE,
            RouteContract.WeatherEntry.COLUMN_SHORT_DESC,
            RouteContract.WeatherEntry.COLUMN_MAX_TEMP,
            RouteContract.WeatherEntry.COLUMN_MIN_TEMP,
            RouteContract.LocationEntry.COLUMN_LOCATION_SETTING,
            RouteContract.WeatherEntry.COLUMN_WEATHER_ID,
            RouteContract.LocationEntry.COLUMN_COORD_LAT,
            RouteContract.LocationEntry.COLUMN_COORD_LONG
    };
    private static final String[] ROUTE_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            RouteContract.RouteEntry.COLUMN_DISTANCE,
            RouteContract.RouteEntry.COLUMN_STATUS,
            RouteContract.RouteEntry.COLUMN_SPEED,
            RouteContract.RouteEntry.COLUMN_TIME,
            RouteContract.RouteEntry.COLUMN_CONSUMPTION,
            RouteContract.RouteEntry.COLUMN_TOTAL_CONSUMPTION,
            RouteContract.RouteEntry.COLUMN_COST,
            RouteContract.RouteEntry.COLUMN_INDICE
    };

    public static final String DATE_FORMAT = "yyyyMMdd";

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;


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
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String noDays = prefs.getString(getString(R.string.pref_noDays_key),
                getString(R.string.pref_noDays_default));
        weatherTask.execute(arrivalCity, noDays);

        String speed = prefs.getString(getString(R.string.pref_speed_key),getString(R.string.pref_speedUnits_km_h));

        FetchRouteTask routeTask = new FetchRouteTask(getActivity());
        routeTask.execute(departureCity,arrivalCity,speed);
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

    private String formatTime(String time)
    {
        String[] parts = time.split("\\.");
        String part1 = parts[0];
        String part2 = parts[1];
        String r = part1 + " h " + part2 + " min";
        return r;
    }
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }
    public static String getFormatDayString(Context context, long dateInMillis) {

            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
    }
    public static String formatTemperature(Context context, double temperature) {
        return context.getString(R.string.format_temperature, temperature);
    }
    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat = R.string.format_wind_kmh;

        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        mRouteAdapter = new RouteAdapter(getActivity(),null,0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(RouteContract.WeatherEntry.buildWeatherLocationWithDate(
                                    arrivalCity, cursor.getLong(COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(ROUTE_LOADER, null, this);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);


        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case FORECAST_LOADER: {
                String sortOrder = RouteContract.WeatherEntry.COLUMN_DATE + " ASC";
                Uri weatherForLocationUri = RouteContract.WeatherEntry.buildWeatherLocationWithStartDate(
                        arrivalCity, System.currentTimeMillis());

                return new CursorLoader(getActivity(),
                        weatherForLocationUri,
                        FORECAST_COLUMNS,
                        null,
                        null,
                        sortOrder);
            }
            case ROUTE_LOADER:
            {
                String sortOrder = RouteContract.RouteEntry.COLUMN_DISTANCE + " ASC";
                Uri routeStartStopUri = RouteContract.RouteEntry.buildRouteStartLocationWithStopLocation(departureCity,arrivalCity);

                return new CursorLoader(getActivity(),
                        routeStartStopUri,
                        ROUTE_COLUMNS,
                        null,
                        null,
                        sortOrder);
            }
            default:return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(loader.getId() == 0) {
            if (!cursor.moveToFirst()) {
                return;
            }
            mForecastAdapter.swapCursor(cursor);
        }
        else
        {
            if (!cursor.moveToFirst())
            {
                return;
            }
            TextView text_dist = (TextView)getView().findViewById(R.id.list_item_distance_textview);

            TextView text_status = (TextView)getView().findViewById(R.id.list_item_status_textview);

            TextView text_speed = (TextView)getView().findViewById(R.id.list_item_speed_textview);

            TextView text_time = (TextView)getView().findViewById(R.id.list_item_time_textview);

            TextView text_cons = (TextView)getView().findViewById(R.id.list_item_consum_textview);

            TextView text_totalCons = (TextView)getView().findViewById(R.id.list_item_consum_total_textview);

            TextView text_cost = (TextView)getView().findViewById(R.id.list_item_cost_textview);

            TextView text_index = (TextView)getView().findViewById(R.id.list_item_index_textview);

            // Extract properties from cursor
            int distance = cursor.getInt(0);
            String status = cursor.getString(1);
            int speed = cursor.getInt(2);
            double time = cursor.getDouble(3);
            int consumption = cursor.getInt(4);
            double tConsumption = cursor.getDouble(5);
            int cost = cursor.getInt(6);
            double indice = cursor.getDouble(7);

            // Populate fields with extracted properties
            text_dist.setText(Integer.toString(distance) + " Km");
            text_status.setText(status);
            //TODO
            //sa utilizez shared preferences pentru a afisa in km/h sau m/s
            text_speed.setText(Integer.toString(speed) + " Km/h");
            text_time.setText(formatTime(Double.toString(time)));
            text_cons.setText(Integer.toString(consumption) + " l/Km");
            text_totalCons.setText(Double.toString(tConsumption)+ " l");
            text_cost.setText(Integer.toString(cost) + " RON");
            text_index.setText(Double.toString(indice));
        }
        //mRouteAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
            mForecastAdapter.swapCursor(null);
            mRouteAdapter.swapCursor(null);
    }



}
