package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import java.util.Set;

public class FavoritesFragment extends Fragment {

    private CalculatorCatalogAdapter adapter;
    private TextView emptyView;

    public FavoritesFragment() {
    }

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyView = view.findViewById(R.id.tv_empty_favorites);
        RecyclerView recyclerView = view.findViewById(R.id.rv_favorites);
        FavoritesRepository favoritesRepository = FavoritesRepository.getInstance(requireContext());

        adapter = new CalculatorCatalogAdapter(favoritesRepository, new CalculatorCatalogAdapter.Listener() {
            @Override
            public void onEntryClick(@NonNull CalculatorCatalog.Entry entry) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToEntry(entry);
                }
            }

            @Override
            public void onFavoriteToggled(@NonNull CalculatorCatalog.Entry entry, boolean isFavorite) {
                loadFavorites();
            }
        }, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        loadFavorites();
        ThemeApplier.apply(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
        if (getView() != null) {
            ThemeApplier.apply(getView());
        }
    }

    private void loadFavorites() {
        FavoritesRepository favoritesRepository = FavoritesRepository.getInstance(requireContext());
        Set<String> favoriteIds = favoritesRepository.getFavoriteIds();
        List<CalculatorCatalog.Entry> favorites = new ArrayList<>();
        for (CalculatorCatalog.Entry entry : CalculatorCatalog.getFavoriteEligibleEntries()) {
            if (favoriteIds.contains(entry.id)) {
                favorites.add(entry);
            }
        }
        adapter.submit(favorites);
        emptyView.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
