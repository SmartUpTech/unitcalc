package net.smartlogic.unitconverter.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.Unit;

import java.util.List;

public class UnitAdapter extends ArrayAdapter<Unit> {

    private LayoutInflater mInflater;
    private List<Unit> units;
    private Context context;

    public UnitAdapter(@NonNull Context context, @NonNull List<Unit> units) {
        super(context, R.layout.single_unit_item, units);
        this.units = units;
        this.context = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            // Inflate the view since it does not exist
            convertView = mInflater.inflate(R.layout.single_unit_item, parent, false);

            // Create and save off the holder in the tag so we get quick access to inner fields
            // This must be done for performance reasons
            holder = new Holder();
            holder.textView = convertView.findViewById(R.id.textView);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        //String s = context.getString(units.get(position).getLabelResource());
        //Log.d("SHRIKI", "unit: " + s);

        holder.textView.setText(context.getString(units.get(position).getLabelResource()));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);

        if (v == null) {
            v = new TextView(context);
        }
        //v.setTextColor(Color.BLACK);
        v.setText(context.getString(units.get(position).getLabelResource()));
        return v;
    }

    private static class Holder {
        public TextView textView;
    }
}
