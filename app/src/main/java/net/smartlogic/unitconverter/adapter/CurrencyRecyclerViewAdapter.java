package net.smartlogic.unitconverter.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.imageview.ShapeableImageView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.Currency;

import java.util.ArrayList;

public class CurrencyRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final ArrayList<Currency> currencies;
    private final Context context;
    private final String type;

    private final ItemClickListener itemClickListener;

    public CurrencyRecyclerViewAdapter(Context context, ArrayList<Currency> currencies, String type, ItemClickListener itemClickListener) {
        this.currencies = currencies;
        this.type = type;
        this.context = context;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_currency_item, parent, false);
        return new MyViewHolder(context, view, type);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (holder instanceof MyViewHolder) {
            Currency currency = currencies.get(position);
            String name = currency.getCurrencyName();
            String symbol = currency.getCurrencySymbol();
            if (symbol != null && !symbol.isEmpty()) {
                ((MyViewHolder) holder).txtCurrency.setText(String.format("%s (%s)", name, symbol));
            } else {
                ((MyViewHolder) holder).txtCurrency.setText(name);
            }
            ((MyViewHolder) holder).txtCurrencyISO.setText(currency.getCurrencyISOCode());
            if (currency.getFlagImageResource() != 123 & currency.getFlagImageResource() != 0) {
                //Log.d("SHRIKI","Here :" + currency.getCountry() + " " + currency.getFlagImageResource());
                ((MyViewHolder) holder).imgFlag.setImageResource(currency.getFlagImageResource());
            }
            ((MyViewHolder) holder).rlSingleItem.setOnClickListener(view -> {
                itemClickListener.onItemClicked(holder, currency, type);
            });
        }

    }

    public static class MyViewHolder extends ViewHolder {
        TextView txtCurrency, txtCurrencyISO;
        ShapeableImageView imgFlag;
        RelativeLayout rlSingleItem;
        public MyViewHolder(final Context context, @NonNull View itemView, final String type) {
            super(itemView);
            txtCurrency = itemView.findViewById(R.id.currency);
            txtCurrencyISO = itemView.findViewById(R.id.currencyISO);
            imgFlag = itemView.findViewById(R.id.flag);
            rlSingleItem = itemView.findViewById(R.id.rl_single_item);
        }
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    public interface ItemClickListener {
        void onItemClicked(ViewHolder vh, Currency currency, String type);
    }
}
