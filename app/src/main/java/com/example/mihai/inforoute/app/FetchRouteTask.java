package com.example.mihai.inforoute.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.mihai.inforoute.app.data.RouteContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by mihai on 5/10/2015.
 */
public class FetchRouteTask extends AsyncTask<String, Void, Void> {

    private final Context mContext;
    public FetchRouteTask(Context context){
        mContext = context;
    }
    private final String LOG_TAG = FetchRouteTask.class.getSimpleName();
    private String formatSpeed(String speed, String unitType)
    {
        int speedInt = Integer.parseInt(speed);
        String r = null;
        if(unitType.equals(mContext.getResources().getString(R.string.pref_speedUnits_m_s)))
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
    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
    long addCity(String cityName)
    {
        long cityId;
        // First, check if the location with this city name exists in the db
        Cursor locationCursor = mContext.getContentResolver().query(
                RouteContract.CityEntry.CONTENT_URI,
                new String[]{RouteContract.CityEntry._ID},
                RouteContract.CityEntry.COLUMN_CITY_NAME + " = ?",
                new String[]{cityName},
                null);

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(RouteContract.CityEntry._ID);
            cityId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            locationValues.put(RouteContract.CityEntry.COLUMN_CITY_NAME, cityName);

            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    RouteContract.CityEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            cityId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        return cityId;

    }

    private void getRouteDataFromJson(String routeJsonStr, String departureCity, String arrivalCity, String speed)
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

        try {

            JSONObject routeJson = new JSONObject(routeJsonStr);
            String resultStrs[] = new String[8];

            long departureCityId = addCity(departureCity);
            long arrivalCityId = addCity(arrivalCity);

            String distance = routeJson.getString(OWM_DISTANTA);
            resultStrs[0] = distance + " Km";
            resultStrs[1] = routeJson.getString(OWM_STATUS);
            String speedJson = routeJson.getString(OWM_VITEZA);
            resultStrs[2] = formatSpeed(speedJson, speed);
            String time = routeJson.getString(OWM_TIMP);
            resultStrs[3] = formatTime(time);
            String consumption = routeJson.getString(OWM_CONSUM);
            resultStrs[4] = consumption + " l/Km";
            String tConsumption = routeJson.getString(OWM_CONSUM_TOTAL);
            resultStrs[5] = tConsumption + " l";
            String cost = routeJson.getString(OWM_COST);
            resultStrs[6] = cost + " RON";
            String indice = routeJson.getString(OWM_INDICE);
            resultStrs[7] = indice;

            ContentValues routeValues = new ContentValues();
            routeValues.put(RouteContract.RouteEntry.COLUMN_START_CITY_KEY,safeLongToInt(departureCityId));
            routeValues.put(RouteContract.RouteEntry.COLUMN_STOP_CITY_KEY,safeLongToInt(arrivalCityId));
            routeValues.put(RouteContract.RouteEntry.COLUMN_DISTANCE,Integer.parseInt(distance));
            routeValues.put(RouteContract.RouteEntry.COLUMN_STATUS,resultStrs[1]);
            routeValues.put(RouteContract.RouteEntry.COLUMN_SPEED,Integer.parseInt(speedJson));
            routeValues.put(RouteContract.RouteEntry.COLUMN_TIME, Double.parseDouble(time));
            routeValues.put(RouteContract.RouteEntry.COLUMN_CONSUMPTION,Integer.parseInt(consumption));
            routeValues.put(RouteContract.RouteEntry.COLUMN_TOTAL_CONSUMPTION,Double.parseDouble(tConsumption));
            routeValues.put(RouteContract.RouteEntry.COLUMN_COST,Integer.parseInt(cost));
            routeValues.put(RouteContract.RouteEntry.COLUMN_INDICE,Double.parseDouble(indice));

            long rowId = 0;

            Uri insertedUri = mContext.getContentResolver().insert(
                    RouteContract.RouteEntry.CONTENT_URI,
                    routeValues
            );
            rowId =  ContentUris.parseId(insertedUri);

            Log.d(LOG_TAG, "FetchRouteTask Complete. " + "Randul "+ rowId);
        }
        catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }
    @Override
    protected Void doInBackground(String... params) {
        Log.v(LOG_TAG, "Params  : " + params[0] + " " + params[1]);
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
            getRouteDataFromJson(routeJsonStr,params[0],params[1],params[2]);
            Log.v(LOG_TAG, "Jsonul route este  : "+routeJsonStr);
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        }
        catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
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
        return null;
    }

//    @Override
//    protected void onPostExecute(String[] result) {
//        if (result != null) {
//            text_dist.setText(result[0]);
//            text_dist.setVisibility(View.VISIBLE);
//
//            text_status.setText(result[1]);
//            text_status.setVisibility(View.VISIBLE);
//
//            text_speed.setText(result[2]);
//            text_speed.setVisibility(View.VISIBLE);
//
//            text_time.setText(result[3]);
//            text_time.setVisibility(View.VISIBLE);
//
//            text_cons.setText(result[4]);
//            text_cons.setVisibility(View.VISIBLE);
//
//            text_totalCons.setText(result[5]);
//            text_totalCons.setVisibility(View.VISIBLE);
//
//            text_cost.setText(result[6]);
//            text_cost.setVisibility(View.VISIBLE);
//
//            text_index.setText(result[7]);
//            text_index.setVisibility(View.VISIBLE);
//        }
//    }
}
