package dev.favourdevlabs.cleanthes.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;

import java.util.ArrayList;
import java.util.List;

public class VaultEntryAdapter extends RecyclerView.Adapter<VaultEntryAdapter.VaultEntryViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(VaultEntry entry);

        void onCopyClick(String password);
    }

    private List<VaultEntry> entries = new ArrayList<>();
    private final OnEntryClickListener listener;
    private final Context context;

    public VaultEntryAdapter(Context context, OnEntryClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public List<VaultEntry> getEntries() {
        return entries;
    }

    public void removeAt(int position) {
        if (position >= 0 && position < entries.size()) {
            entries.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, entries.size());
        }
    }

    public void insertAt(int position, VaultEntry entry) {
        if (position >= 0 && position <= entries.size()) {
            entries.add(position, entry);
            notifyItemInserted(position);
        }
    }

    @NonNull
    @Override
    public VaultEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cleanthes_entry, parent, false);
        return new VaultEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaultEntryViewHolder holder, int position) {
        holder.bind(entries.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void submitList(List<VaultEntry> newEntries) {
        final List<VaultEntry> safeNew = (newEntries != null) ? newEntries : new ArrayList<>();
        final List<VaultEntry> oldEntries = this.entries;

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldEntries.size();
            }

            @Override
            public int getNewListSize() {
                return safeNew.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldEntries.get(oldPos).getId() == safeNew.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                VaultEntry o = oldEntries.get(oldPos);
                VaultEntry n = safeNew.get(newPos);
                return o.getTitle().equals(n.getTitle())
                        && o.getUsername().equals(n.getUsername())
                        && o.getCategory().equals(n.getCategory())
                        && o.isFavorite() == n.isFavorite()
                        && o.hasTOTP() == n.hasTOTP(); // rebind when TOTP added/removed
            }
        });

        this.entries = new ArrayList<>(safeNew);
        result.dispatchUpdatesTo(this);
    }

    // -------------------------------------------------------------------------

    static class VaultEntryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvInitial;
        private final TextView tvTitle;
        private final TextView tvUsername;
        private final View iconBg;
        private final View totpDot;
        private final ImageButton btnCopy;

        VaultEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.item_tv_initial);
            tvTitle = itemView.findViewById(R.id.item_tv_title);
            tvUsername = itemView.findViewById(R.id.item_tv_username);
            iconBg = itemView.findViewById(R.id.item_icon_bg);
            totpDot = itemView.findViewById(R.id.item_totp_dot);
            btnCopy = itemView.findViewById(R.id.item_btn_copy);
        }

        void bind(VaultEntry entry, OnEntryClickListener listener) {
            tvTitle.setText(entry.getTitle());
            tvUsername.setText(entry.getUsername());

            String initial = entry.getTitle().isEmpty() ? "?"
                    : String.valueOf(entry.getTitle().charAt(0)).toUpperCase();
            tvInitial.setText(initial);

            int circleColor = entry.isFavorite()
                    ? Color.parseColor("#FFB300")
                    : categoryColor(entry.getCategory());
            iconBg.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(circleColor));

            // Gold dot at top-right of avatar — tells user this entry has TOTP
            totpDot.setVisibility(entry.hasTOTP() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onEntryClick(entry));
            btnCopy.setOnClickListener(v -> listener.onCopyClick(entry.getEncryptedPassword()));
        }

        private static int categoryColor(String category) {
            int[] palette = {
                    Color.parseColor("#2E86AB"),
                    Color.parseColor("#A23B72"),
                    Color.parseColor("#F18F01"),
                    Color.parseColor("#3DAA6E"),
            };
            return palette[Math.abs(category.hashCode()) % palette.length];
        }
    }
}
