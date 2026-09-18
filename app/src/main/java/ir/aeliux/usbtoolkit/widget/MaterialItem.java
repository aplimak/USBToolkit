package ir.aeliux.usbtoolkit.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import ir.aeliux.usbtoolkit.R;
import ir.aeliux.usbtoolkit.databinding.ViewMaterialItemBinding;

public class MaterialItem extends ConstraintLayout {
    private ViewMaterialItemBinding binding;

    private boolean hasChevron = false;
    private boolean masterListenerEnabled = true;
    private CharSequence[] dropdownEntries;

    private OnClickListener clickListener;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private DialogInterface.OnClickListener dropdownItemSelectedListener;
    private boolean hasSwitch;
    private Drawable originalItemBackground;
    private Drawable originalSwitchBackground;

    private String title;
    private String subtitle;
    private String altSubtitle;  // Used for selected drop down item, so we don't lose the original subtitle
    private int iconResource;
    private int selectedIndex;
    private boolean checked;

    public MaterialItem(@NonNull Context context) {
        this(context, null);
    }
    public MaterialItem(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public MaterialItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr);init(context, attrs); }

    private void init(Context context, AttributeSet attrs) {
        binding = ViewMaterialItemBinding.inflate(LayoutInflater.from(context), this);

        originalItemBackground = getBackground();
        originalSwitchBackground = binding.switchWidget.getBackground();
        binding.switchWidget.setOnCheckedChangeListener((btn, isChecked) -> {
            if (checked == isChecked) return;  // Don't fire event when syncing or when nothing is changed
            checked = isChecked;  // Doesn't need refresh
            if (hasSwitch && checkedChangeListener != null) checkedChangeListener.onCheckedChanged(btn, isChecked);
        });

        super.setOnClickListener(this::masterListener);

        // Base padding to match Material 3 settings rows
        setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        binding.textContainer.setMinimumHeight(dpToPx(48));

        // NOTE: This part wont be executed on runtime created widgets
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialItem)) {
                applyAttribute(context, a);
            }
        }

        post(this::refresh);
    }

    private void applyAttribute(Context context, TypedArray attributes) {
        title = attributes.getString(R.styleable.MaterialItem_itemTitle);
        subtitle = attributes.getString(R.styleable.MaterialItem_itemSubtitle);
        iconResource = attributes.getResourceId(R.styleable.MaterialItem_itemIcon, 0);

        masterListenerEnabled = attributes.getBoolean(R.styleable.MaterialItem_attachMasterListener, true);
        hasSwitch = attributes.getBoolean(R.styleable.MaterialItem_showSwitch, false);
        checked = attributes.getBoolean(R.styleable.MaterialItem_isChecked, false);
        int dropdownEntriesRes = attributes.getResourceId(R.styleable.MaterialItem_dropdownEntries, 0);
        if (dropdownEntriesRes != 0) {
            dropdownEntries = context.getResources().getTextArray(dropdownEntriesRes);
        } else {
            dropdownEntries = new CharSequence[0];
        }
        hasChevron = attributes.getBoolean(R.styleable.MaterialItem_showChevron, false);
    }

    private void refresh() {
        binding.title.setText(title);

        boolean hasIcon = iconResource != 0;
        binding.icon.setImageResource(iconResource);
        binding.icon.setVisibility(hasIcon ? View.VISIBLE : View.GONE);

        boolean chevron = shouldShowChevron();
        var textContainerLayoutParams = binding.textContainer.getLayoutParams();
        ((MarginLayoutParams)textContainerLayoutParams).setMarginStart(hasIcon ? dpToPx(24) : 0);
        ((LayoutParams)textContainerLayoutParams).startToEnd = hasIcon ? binding.icon.getId() : LayoutParams.UNSET;
        ((LayoutParams)textContainerLayoutParams).startToStart = hasIcon ? LayoutParams.UNSET : LayoutParams.PARENT_ID;
        binding.textContainer.setLayoutParams(textContainerLayoutParams);

        binding.chevron.setVisibility(chevron ? View.VISIBLE : View.GONE);
        binding.divider.setVisibility(chevron ? View.VISIBLE : View.GONE);

        boolean clickable = masterListenerEnabled || clickListener != null;
        setClickable(clickable);
        setFocusable(clickable);

        refreshSwitch();
        refreshDropdown();

        String selectedSubtitle = getDisplaySubtitle();
        binding.subtitle.setText(selectedSubtitle);
        binding.subtitle.setVisibility(selectedSubtitle == null || selectedSubtitle.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshSwitch() {
        binding.switchWidget.setVisibility(hasSwitch ? View.VISIBLE : View.GONE);
        if (!hasSwitch) return;

        binding.switchWidget.setChecked(checked);

        boolean chevron = shouldShowChevron();
        binding.switchWidget.setClickable(!masterListenerEnabled || chevron);
        binding.switchWidget.setFocusable(!masterListenerEnabled || chevron);
        binding.switchWidget.setDuplicateParentStateEnabled(chevron);
        binding.switchWidget.setBackground(masterListenerEnabled && !chevron ? null : originalSwitchBackground);
    }

    private void refreshDropdown() {
        if (!hasDropdown()) {
            altSubtitle = null;
            return;
        }

        boolean chevron = shouldShowChevron();
        if (!masterListenerEnabled) {
            throw new IllegalStateException("Dropdown requires attachMasterListener to handle clicks");
        }
        if (hasSwitch && !chevron) {
            throw new IllegalStateException("Can't have dropdown and switch at the same time, either disable one or enable chevron to handle both.");
        }

        if (selectedIndex < 0 || selectedIndex >= dropdownEntries.length) {
            Log.w("MaterialItem", "SelectedIndex out of range of the current dropdown entries, index: " + selectedIndex + ", entries length: " + dropdownEntries.length + ". resetting index to 0");  // Not a big deal but at least we inform the developer
            selectedIndex = 0;
        }

        altSubtitle = dropdownEntries[selectedIndex].toString();
    }

    private boolean shouldShowChevron() {
        return hasChevron && masterListenerEnabled && hasSwitch;
    }

    private boolean hasDropdown() {
        return dropdownEntries != null && dropdownEntries.length > 0;
    }

    private void masterListener(View v) {
        if (!masterListenerEnabled) {
            fireClickListener();
            return;
        }

        boolean dropdownEnabled = hasDropdown();
        if (shouldShowChevron() && !dropdownEnabled) {
            fireClickListener();
        } else if (dropdownEnabled) {
            showDropdown();
        } else if (hasSwitch) {
            binding.switchWidget.setChecked(!binding.switchWidget.isChecked());
        } else {
            fireClickListener();
        }
    }

    private void fireClickListener() {
        if (clickListener != null) {
            clickListener.onClick(this);
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        applySelectableBackground(this, clickable);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        clickListener = l;
        refresh();
    }

    public void setOnCheckedChangeListener(@Nullable CompoundButton.OnCheckedChangeListener l) {
        checkedChangeListener = l;
    }

    public void setOnDropdownItemSelectedListener(@Nullable DialogInterface.OnClickListener l) {
        dropdownItemSelectedListener = l;
    }

    private void applySelectableBackground(View view, boolean enable) {
        if (!enable) {
            view.setBackground(originalItemBackground);
            return;
        }
        TypedValue outValue = new TypedValue();
        boolean resolved = getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
        );
        if (resolved && outValue.resourceId != 0) {
            view.setBackgroundResource(outValue.resourceId);
        }
    }

    private void showDropdown() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle(binding.title.getText())
                .setSingleChoiceItems(dropdownEntries, selectedIndex, (dialog, which) -> {
                    setSelectedItemInternal(which, false);
                    dialog.dismiss();
                })
                .show();
    }

    private void setSelectedItemInternal(int which, boolean forced) {
        if (!forced && selectedIndex == which) return;
        selectedIndex = which;
        refresh();
        if (!forced && dropdownItemSelectedListener != null) {
            dropdownItemSelectedListener.onClick(null, which);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public void setTitle(CharSequence text) {
        title = text == null ? null : text.toString();
        refresh();
    }
    public CharSequence getTitle() {
        return title;
    }
    public void setSubtitle(CharSequence text) {
        subtitle = text == null ? null : text.toString();
        refresh();
    }
    public CharSequence getSourceSubtitle() {
        return subtitle;
    }
    public String getDisplaySubtitle() {
        return altSubtitle != null && !altSubtitle.isEmpty() ? altSubtitle : subtitle;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        refresh();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setDropdownEntries(CharSequence[] entries) {
        if (entries != null) {
            for (CharSequence e : entries) {
                if (e == null) throw new IllegalArgumentException("Dropdown entries must not be null");
            }
        }
        dropdownEntries = entries;
        refresh();
    }
    public CharSequence[] getDropdownEntries() {
        return this.dropdownEntries.clone();
    }
    public int getSelectedItemIndex() {
        return selectedIndex;
    }
    public CharSequence getSelectedItem() {
        return dropdownEntries[selectedIndex];
    }

    public void setSelectedItem(int index) {
        if (index < 0 || index >= dropdownEntries.length) {
            throw new IndexOutOfBoundsException();
        }
        setSelectedItemInternal(index, false);
    }

    public void setIconResource(int iconRes) {
        iconResource = iconRes;
        refresh();
    }

    public void setShowSwitch(boolean enable) {
        if (hasSwitch == enable) return;
        hasSwitch = enable;
        refresh();
    }

    public void setShowChevron(boolean enable) {
        if (hasChevron == enable) return;
        hasChevron = enable;
        refresh();
    }

    public void setAttachMasterListener(boolean enable) {
        if (masterListenerEnabled == enable) return;
        masterListenerEnabled = enable;
        refresh();
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.checked = checked;
        ss.selectedItemIndex = selectedIndex;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        checked = ss.checked;
        selectedIndex = ss.selectedItemIndex;

        post(this::refresh);
    }

    static class SavedState extends BaseSavedState {
        boolean checked;
        int selectedItemIndex;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = in.readInt() == 1;
            selectedItemIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(checked ? 1 : 0);
            out.writeInt(selectedItemIndex);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
