package net.smartlogic.unitconverter.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.utils.Conversions;

public class ConversionAdapter extends ArrayAdapter<Integer> {

    private final LayoutInflater mInflater;
    private final Integer[] val;
    private final Context context;
    private final Conversions conversions;


    public ConversionAdapter(@NonNull Context context, Integer[] values) {
        super(context, R.layout.single_item,values);
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.val = values;
        this.conversions = Conversions.getInstance();
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getView(position, convertView, parent);

        Holder holder;

        if (convertView == null) {
            // Inflate the view since it does not exist
            convertView = mInflater.inflate(R.layout.single_item, parent, false);

            // Create and save off the holder in the tag so we get quick access to inner fields
            // This must be done for performance reasons
            holder = new Holder();
            holder.textView = convertView.findViewById(R.id.textView);
            holder.imageView = convertView.findViewById(R.id.image);
            holder.ll = convertView.findViewById(R.id.ll);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        // Populate the text
        holder.textView.setText(context.getString(conversions.getById(val[position]).getLabelResource()));
        holder.imageView.setImageResource(conversions.getById(val[position]).getImageResource());


        if (position == selectedItem){
            holder.ll.setBackgroundColor(context.getResources().getColor(R.color.colorHighlighter));
        }
        else{
            holder.ll.setBackgroundColor(context.getResources().getColor(R.color.colorNewGrey));
        }

        return convertView;
    }

    /** View holder for the views we need access to */
    private static class Holder {
        public TextView textView;
        ImageView imageView;
        LinearLayout ll;
    }

    private int selectedItem;

    public void setSelectedItem(int position) {
        selectedItem = position;
    }
}
