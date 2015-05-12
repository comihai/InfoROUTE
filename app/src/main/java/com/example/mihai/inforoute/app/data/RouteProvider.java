package com.example.mihai.inforoute.app.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by mihai on 5/9/2015.
 */
public class RouteProvider extends ContentProvider{

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private RouteDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;
    static final int CITY = 400;
    static final int CITY_NAME = 401;
    static final int ROUTE = 500;
    static final int ROUTE_START_STOP = 501;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder, sRouteByCityNameQueryBuilder;

    static
    {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sWeatherByLocationSettingQueryBuilder.setTables(
                RouteContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        RouteContract.LocationEntry.TABLE_NAME +
                        " ON " + RouteContract.WeatherEntry.TABLE_NAME +
                        "." + RouteContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + RouteContract.LocationEntry.TABLE_NAME +
                        "." + RouteContract.LocationEntry._ID);
    }

    static
    {
        sRouteByCityNameQueryBuilder = new SQLiteQueryBuilder();

        //2 inner join
//        sRouteByCityNameQueryBuilder.setTables(
//                RouteContract.RouteEntry.TABLE_NAME + " INNER JOIN " +
//                        RouteContract.CityEntry.TABLE_NAME +
//                        " ON " + RouteContract.RouteEntry.TABLE_NAME +
//                        "." + RouteContract.RouteEntry.COLUMN_START_CITY_KEY +
//                        " = " + RouteContract.CityEntry.TABLE_NAME +
//                        "." + RouteContract.CityEntry._ID
//                        + " INNER JOIN " +
//                        RouteContract.CityEntry.TABLE_NAME +
//                        " ON " + RouteContract.RouteEntry.TABLE_NAME +
//                        "." + RouteContract.RouteEntry.COLUMN_STOP_CITY_KEY +
//                        " = " + RouteContract.CityEntry.TABLE_NAME +
//                        "." + RouteContract.CityEntry._ID);
        sRouteByCityNameQueryBuilder.setTables(
                RouteContract.RouteEntry.TABLE_NAME + " a INNER JOIN " +
                        RouteContract.CityEntry.TABLE_NAME +
                        " b ON a"+ "." + RouteContract.RouteEntry.COLUMN_START_CITY_KEY +
                        " = b" + "." + RouteContract.CityEntry._ID
                        + " INNER JOIN " +
                        RouteContract.CityEntry.TABLE_NAME +
                        " c ON a" +"." + RouteContract.RouteEntry.COLUMN_STOP_CITY_KEY +
                        " = c" +"." + RouteContract.CityEntry._ID);
    }

    //location.location_setting = ?
    private static final String sLocationSettingSelection =
            RouteContract.LocationEntry.TABLE_NAME+
                    "." + RouteContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    //location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            RouteContract.LocationEntry.TABLE_NAME+
                    "." + RouteContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    RouteContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sLocationSettingAndDaySelection =
            RouteContract.LocationEntry.TABLE_NAME +
                    "." + RouteContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    RouteContract.WeatherEntry.COLUMN_DATE + " = ? ";

    //orase.nume_oras = ? AND orase.nume_oras = ?
    private static final String sStartCityAndFinishCitySelection =
                    "b." + RouteContract.CityEntry.COLUMN_CITY_NAME + " = ? AND " +
                    "c." +RouteContract.CityEntry.COLUMN_CITY_NAME + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = RouteContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = RouteContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(
                //baza de date pe care se face query
                mOpenHelper.getReadableDatabase(),
                //care coloane sa fie returnate
                projection,
                //care randuri sa fie returnate - sql where
                selection,
                //se inlocuiesc semnele '?' din selection cu argumente din selectionArgs
                selectionArgs,
                //group by
                null,
                //having
                null,
                //order by
                sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = RouteContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date = RouteContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getRouteByStartCityAndFinishCity(Uri uri, String[] projection, String sortOrder) {
        String startCity = RouteContract.WeatherEntry.getStartCityFromUri(uri);
        String finishCity = RouteContract.WeatherEntry.getStopCityFromUri(uri);

        return sRouteByCityNameQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                sStartCityAndFinishCitySelection,
                new String[]{startCity, finishCity},
                null,
                null,
                sortOrder
        );
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RouteContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, RouteContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, RouteContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, RouteContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, RouteContract.PATH_LOCATION, LOCATION);

        matcher.addURI(authority, RouteContract.PATH_CITY, CITY);
        matcher.addURI(authority, RouteContract.PATH_CITY + "/*", CITY_NAME);
        matcher.addURI(authority, RouteContract.PATH_ROUTE, ROUTE);
        matcher.addURI(authority, RouteContract.PATH_ROUTE + "/*/*", ROUTE_START_STOP);
        return matcher;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(RouteContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(RouteContract.WeatherEntry.COLUMN_DATE);
            values.put(RouteContract.WeatherEntry.COLUMN_DATE, RouteContract.normalizeDate(dateValue));
        }
    }


    @Override
    public boolean onCreate() {
        mOpenHelper = new RouteDbHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RouteContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RouteContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "city"
            case CITY: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RouteContract.CityEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "route"
            case ROUTE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RouteContract.RouteEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "route/*/*"
            case ROUTE_START_STOP:
            {
                retCursor = getRouteByStartCityAndFinishCity(uri, projection, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case WEATHER_WITH_LOCATION_AND_DATE:
                //returneaze un singur rand
                return RouteContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                //returneaza mai multe randuri
                return RouteContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return RouteContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return RouteContract.LocationEntry.CONTENT_TYPE;
            case CITY:
                return RouteContract.CityEntry.CONTENT_TYPE;
            case CITY_NAME:
                return RouteContract.CityEntry.CONTENT_ITEM_TYPE;
            case ROUTE:
                return RouteContract.RouteEntry.CONTENT_TYPE;
            case ROUTE_START_STOP:
                return RouteContract.RouteEntry.CONTENT_ITEM_TYPE;


            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER: {
                normalizeDate(values);
                long _id = db.insert(RouteContract.WeatherEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = RouteContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                long _id = db.insert(RouteContract.LocationEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = RouteContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CITY: {
                long _id = db.insert(RouteContract.CityEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = RouteContract.CityEntry.buildCityUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ROUTE: {
                long _id = db.insert(RouteContract.RouteEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = RouteContract.RouteEntry.buildRouteUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(
                        RouteContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(
                        RouteContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
            case CITY:
                rowsDeleted = db.delete(
                        RouteContract.CityEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ROUTE:
                rowsDeleted = db.delete(
                        RouteContract.RouteEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case WEATHER:
                normalizeDate(values);
                rowsUpdated = db.update(RouteContract.WeatherEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(RouteContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case CITY:
                normalizeDate(values);
                rowsUpdated = db.update(RouteContract.CityEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case ROUTE:
                rowsUpdated = db.update(RouteContract.RouteEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(RouteContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
