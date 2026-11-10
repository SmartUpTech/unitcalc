package net.smartlogic.unitconverter.helper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple ViewPager2 adapter for workspace tab panes (Calculate/Graphy/History).
 */
public class WorkspacePagerAdapter extends RecyclerView.Adapter<WorkspacePagerAdapter.PageHolder> {

    public interface PageBindListener {
        void onPageBound(int position, @NonNull View pageView);
    }

    private final int[] layoutResIds;
    private final PageBindListener listener;
    private final LayoutInflater inflater;

    public WorkspacePagerAdapter(@NonNull @LayoutRes int[] layoutResIds,
                                 @NonNull PageBindListener listener) {
        this(layoutResIds, listener, null);
    }

    public WorkspacePagerAdapter(@NonNull @LayoutRes int[] layoutResIds,
                                 @NonNull PageBindListener listener,
                                 @Nullable LayoutInflater inflater) {
        this.layoutResIds = layoutResIds;
        this.listener = listener;
        this.inflater = inflater;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Fragment-owned inflaters attach XML child fragments to their calculator owner.
        View view = (inflater != null ? inflater : LayoutInflater.from(parent.getContext()))
                .inflate(layoutResIds[viewType], parent, false);
        return new PageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        listener.onPageBound(position, holder.itemView);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return layoutResIds.length;
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        PageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
