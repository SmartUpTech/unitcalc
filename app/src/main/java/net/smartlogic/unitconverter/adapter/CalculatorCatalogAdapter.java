package net.smartlogic.unitconverter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.FavoritesRepository;
import net.smartlogic.unitconverter.model.CalculatorCatalog;

import java.util.ArrayList;
import java.util.List;

public class CalculatorCatalogAdapter extends RecyclerView.Adapter<CalculatorCatalogAdapter.Holder> {

    public interface Listener {
        void onEntryClick(@NonNull CalculatorCatalog.Entry entry);

        void onFavoriteToggled(@NonNull CalculatorCatalog.Entry entry, boolean isFavorite);
    }

    private final List<CalculatorCatalog.Entry> entries = new ArrayList<>();
    private final FavoritesRepository favoritesRepository;
    private final Listener listener;
    private final boolean showFavoriteButton;

    public CalculatorCatalogAdapter(@NonNull FavoritesRepository favoritesRepository,
                                    @NonNull Listener listener,
                                    boolean showFavoriteButton) {
        this.favoritesRepository = favoritesRepository;
        this.listener = listener;
        this.showFavoriteButton = showFavoriteButton;
    }

    public void submit(@NonNull List<CalculatorCatalog.Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calculator_catalog, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CalculatorCatalog.Entry entry = entries.get(position);
        holder.title.setText(entry.titleRes);
        holder.icon.setImageResource(entry.iconRes);

        if (showFavoriteButton && entry.isFavoriteEligible()) {
            holder.favorite.setVisibility(View.VISIBLE);
            boolean isFavorite = favoritesRepository.isFavorite(entry.id);
            int tint = ContextCompat.getColor(holder.itemView.getContext(),
                    isFavorite ? R.color.accent : R.color.screen_expression_text);
            holder.favorite.setColorFilter(tint);
            holder.favorite.setOnClickListener(v -> {
                boolean next = favoritesRepository.toggleFavorite(entry.id);
                listener.onFavoriteToggled(entry, next);
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
        } else {
            holder.favorite.setVisibility(View.GONE);
            holder.favorite.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> listener.onEntryClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final ImageButton favorite;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.catalog_icon);
            title = itemView.findViewById(R.id.catalog_title);
            favorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}
