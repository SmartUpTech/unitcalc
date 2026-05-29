package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.adapter.CurrencyRecyclerViewAdapter;
import net.smartlogic.unitconverter.adapter.CurrencyRecyclerViewAdapter.ItemClickListener;
import net.smartlogic.unitconverter.model.Currency;

import java.util.ArrayList;

public class BottomSheetCurrencyDialogFragment extends BottomSheetDialogFragment {

    private final ArrayList<Currency> curr;
    private final String type;

    OnChooseCurrencyListener listener;

    public BottomSheetCurrencyDialogFragment(OnChooseCurrencyListener listener, ArrayList<Currency> curr, String type) {
        this.curr = curr;
        this.type = type;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_currency, container, false);

        ItemClickListener itemClickListener = (vh, currency, type) -> {
            //Log.d("SHRIKI","Clicked currency -> " + currency.getCountry());
            listener.onChooseCurrency(currency, type);
        };

        RecyclerView recyclerView = view.findViewById(R.id.rvCurrencies);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        CurrencyRecyclerViewAdapter currencyRecyclerViewAdapter = new CurrencyRecyclerViewAdapter(getActivity(), curr, type, itemClickListener);
        recyclerView.setAdapter(currencyRecyclerViewAdapter);
        return view;
    }

    public interface OnChooseCurrencyListener {
        void onChooseCurrency(Currency c, String type);
    }
}
