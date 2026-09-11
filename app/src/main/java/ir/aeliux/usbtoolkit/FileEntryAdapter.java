package ir.aeliux.usbtoolkit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FileEntryAdapter extends ListAdapter<FileEntry, FileEntryAdapter.ViewHolder> {

    public interface OnDirectoryClick { void onClick(FileEntry entry); }
    public interface OnFileSelect { void onSelect(FileEntry entry, boolean checked); }
    public interface OnFileClick { void onClick(FileEntry entry); }

    private final OnDirectoryClick onDirectoryClick;
    private final OnFileSelect onFileSelect;
    private final OnFileClick onFileClick;
    private final boolean allowMultiple;
    private final Set<String> allowedExtensions;

    private final Set<String> selectedSet = new HashSet<>();

    private static final DiffUtil.ItemCallback<FileEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FileEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull FileEntry oldItem, @NonNull FileEntry newItem) {
                    return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FileEntry oldItem, @NonNull FileEntry newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public FileEntryAdapter(OnDirectoryClick onDirectoryClick,
                            OnFileSelect onFileSelect,
                            OnFileClick onFileClick,
                            boolean allowMultiple,
                            Set<String> allowedExtensions) {
        super(DIFF_CALLBACK);
        this.onDirectoryClick = onDirectoryClick;
        this.onFileSelect = onFileSelect;
        this.onFileClick = onFileClick;
        this.allowMultiple = allowMultiple;
        this.allowedExtensions = allowedExtensions;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvName;
        final TextView tvDetails;
        final MaterialCheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileEntry entry = getItem(position);

        holder.tvName.setText(entry.getName());

        // Reset listener to avoid recycled-view leakage
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.itemView.setOnClickListener(null);

        if (entry.isDirectory()) {
            holder.ivIcon.setImageResource(R.drawable.ic_folder);
            holder.tvDetails.setText("Directory");
            holder.cbSelect.setVisibility(View.GONE);

            holder.itemView.setAlpha(1f);
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> onDirectoryClick.onClick(entry));
            return;
        }

        holder.ivIcon.setImageResource(R.drawable.ic_file);
        holder.tvDetails.setText(
                formatSize(entry.getSize())
                        + "  •  " + formatDate(entry.getLastModified())
        );

        String ext = "";
        int dot = entry.getName().lastIndexOf('.');
        if (dot >= 0 && dot < entry.getName().length() - 1) {
            ext = entry.getName().substring(dot + 1).toLowerCase(Locale.ROOT);
        }

        boolean isAllowed = (allowedExtensions == null) || allowedExtensions.contains(ext);

        if (!isAllowed) {
            holder.itemView.setAlpha(0.4f);
            holder.itemView.setEnabled(false);
            holder.cbSelect.setVisibility(View.GONE);
            return;
        }

        holder.itemView.setAlpha(1f);
        holder.itemView.setEnabled(true);
        holder.cbSelect.setVisibility(allowMultiple ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedSet.contains(entry.getAbsolutePath()));

        holder.itemView.setOnClickListener(v -> onFileClick.onClick(entry));

        holder.cbSelect.setOnCheckedChangeListener(
                (buttonView, isChecked) -> onFileSelect.onSelect(entry, isChecked));
    }

    public void setSelected(String path, boolean selected) {
        if (selected) selectedSet.add(path);
        else selectedSet.remove(path);
        notifyDataSetChanged();
    }

    public void deselectAll() {
        selectedSet.clear();
        notifyDataSetChanged();
    }

    public void restoreSelection(Set<String> paths) {
        selectedSet.clear();
        selectedSet.addAll(paths);
        notifyDataSetChanged();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}