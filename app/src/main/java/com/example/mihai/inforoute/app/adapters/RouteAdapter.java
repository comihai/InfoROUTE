package com.example.mihai.inforoute.app.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mihai.inforoute.app.R;

/**
 * Created by mihai on 5/10/2015.
 */
public class RouteAdapter extends CursorAdapter{

    public RouteAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private String formatTime(String time)
    {
        String[] parts = time.split("\\.");
        String part1 = parts[0];
        String part2 = parts[1];
        String r = part1 + " h " + part2 + " min";
        return r;
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.fragment_main, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView text_dist = (TextView)view.findViewById(R.id.list_item_distance_textview);

        TextView text_status = (TextView)view.findViewById(R.id.list_item_status_textview);

        TextView text_speed = (TextView)view.findViewById(R.id.list_item_speed_textview);

        TextView text_time = (TextView)view.findViewById(R.id.list_item_time_textview);

        TextView text_cons = (TextView)view.findViewById(R.id.list_item_consum_textview);

        TextView text_totalCons = (TextView)view.findViewById(R.id.list_item_consum_total_textview);

        TextView text_cost = (TextView)view.findViewById(R.id.list_item_cost_textview);

        TextView text_index = (TextView)view.findViewById(R.id.list_item_index_textview);

        // Extract properties from cursor
        int distance = cursor.getInt(cursor.getColumnIndexOrThrow("distance"));
        String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
        int speed = cursor.getInt(cursor.getColumnIndexOrThrow("speed"));
        double time = cursor.getDouble(cursor.getColumnIndexOrThrow("time"));
        int consumption = cursor.getInt(cursor.getColumnIndexOrThrow("consumption"));
        double tConsumption = cursor.getDouble(cursor.getColumnIndexOrThrow("total_consumption"));
        int cost = cursor.getInt(cursor.getColumnIndexOrThrow("cost"));
        double indice = cursor.getDouble(cursor.getColumnIndexOrThrow("indice"));

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
}
