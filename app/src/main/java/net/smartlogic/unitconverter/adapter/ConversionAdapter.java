package net.smartlogic.unitconverter.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.utils.Conversions;

public class ConversionAdapter extends ArrayAdapter<Integer> {

    private final LayoutInflater mInflater;
    private final Integer[] val;
    private final Context context;
    private final Conversions conversions;


    public ConversionAdapter(@NonNull Context context, Integer[] values) {
        super(context, R.layout.single_item, values);
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.val = values;
        this.conversions = Conversions.getInstance();
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.single_item, parent, false);
            holder = new Holder();
            holder.textView = convertView.findViewById(R.id.textView);
            holder.imageView = convertView.findViewById(R.id.image);
            holder.iconContainer = convertView.findViewById(R.id.icon_container);
            holder.indicator = convertView.findViewById(R.id.indicator);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.textView.setText(context.getString(conversions.getById(val[position]).getLabelResource()));
        holder.imageView.setImageResource(conversions.getById(val[position]).getImageResource());

        boolean selected = position == selectedItem;
        convertView.setSelected(selected);
        holder.indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        holder.iconContainer.setBackgroundResource(
                selected ? R.drawable.bg_category_icon_selected : R.drawable.bg_category_icon_default);
        int accent = ContextCompat.getColor(context, R.color.accent);
        int muted = ContextCompat.getColor(context, R.color.screen_expression_text);
        holder.textView.setTextColor(selected ? accent : muted);
        holder.imageView.setColorFilter(selected ? accent : muted);

        return convertView;
    }

    private static class Holder {
        TextView textView;
        ImageView imageView;
        View iconContainer;
        View indicator;
    }

    private int selectedItem;

    public void setSelectedItem(int position) {
        selectedItem = position;
    }
}
