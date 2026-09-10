package net.smartlogic.unitconverter.theme;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import net.smartlogic.unitconverter.R;

import java.util.List;

public final class ThemeChipAdapter extends RecyclerView.Adapter<ThemeChipAdapter.Holder> {

    public interface OnThemeClickListener {
        void onThemeClicked(@NonNull CalculatorTheme theme);
    }

    private final List<CalculatorTheme> themes;
    private final OnThemeClickListener listener;
    @NonNull
    private String selectedId;

    public ThemeChipAdapter(
            @NonNull List<CalculatorTheme> themes,
            @NonNull String selectedId,
            @NonNull OnThemeClickListener listener
    ) {
        this.themes = themes;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public void setSelectedId(@NonNull String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme_chip, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CalculatorTheme theme = themes.get(position);
        holder.bind(theme, theme.id.equals(selectedId), listener);
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tv_theme_name);
        }

        void bind(@NonNull CalculatorTheme theme, boolean selected,
                  @NonNull OnThemeClickListener listener) {
            label.setText(theme.name);
            label.setTextColor(theme.mainText);
            GradientDrawable background = new GradientDrawable();
            background.setColor(theme.background);
            background.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 12f);
            int stroke = selected ? Math.round(itemView.getResources().getDisplayMetrics().density * 2f) : 0;
            if (selected) {
                int outline = ColorUtils.setAlphaComponent(theme.mainText, 230);
                background.setStroke(stroke, outline);
            } else {
                background.setStroke(0, 0);
            }
            itemView.setBackground(background);
            itemView.setOnClickListener(v -> listener.onThemeClicked(theme));
        }
    }
}
