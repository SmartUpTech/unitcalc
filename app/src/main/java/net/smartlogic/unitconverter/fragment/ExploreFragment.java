package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.adapter.CalculatorCatalogAdapter;
import net.smartlogic.unitconverter.helper.FavoritesRepository;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private CalculatorCatalogAdapter adapter;
    private final List<CalculatorCatalog.Entry> allEntries = new ArrayList<>();

    public ExploreFragment() {
    }

    public static ExploreFragment newInstance() {
        return new ExploreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allEntries.clear();
        for (CalculatorCatalog.Entry entry : CalculatorCatalog.getFavoriteEligibleEntries()) {
            allEntries.add(entry);
        }

        RecyclerView recyclerView = view.findViewById(R.id.rv_explore);
        EditText search = view.findViewById(R.id.explore_search);
        FavoritesRepository favoritesRepository = FavoritesRepository.getInstance(requireContext());

        adapter = new CalculatorCatalogAdapter(favoritesRepository, new CalculatorCatalogAdapter.Listener() {
            @Override
            public void onEntryClick(@NonNull CalculatorCatalog.Entry entry) {
                navigate(entry);
            }

            @Override
            public void onFavoriteToggled(@NonNull CalculatorCatalog.Entry entry, boolean isFavorite) {
            }
        }, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.submit(allEntries);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterEntries(s.toString());
            }
        });
        ThemeApplier.apply(view);
    }

    private void filterEntries(@NonNull String query) {
        if (query.trim().isEmpty()) {
            adapter.submit(allEntries);
            return;
        }
        String lower = query.toLowerCase();
        List<CalculatorCatalog.Entry> filtered = new ArrayList<>();
        for (CalculatorCatalog.Entry entry : allEntries) {
            String title = getString(entry.titleRes).toLowerCase();
            if (title.contains(lower)) {
                filtered.add(entry);
            }
        }
        adapter.submit(filtered);
    }

    private void navigate(@NonNull CalculatorCatalog.Entry entry) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToEntry(entry);
        }
    }
}
