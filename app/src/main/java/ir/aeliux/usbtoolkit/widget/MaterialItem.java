package ir.aeliux.usbtoolkit.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.materialswitch.MaterialSwitch;

import ir.aeliux.usbtoolkit.R;

public class MaterialItem extends ConstraintLayout {
    private LinearLayout textContainer;
    private TextView title, subtitle;
    private ImageView icon, chevron;
    private View divider;
    private MaterialSwitch switchWidget;

    private boolean hasChevron = false;
    private boolean masterListenerEnabled = false;
    private CharSequence[] dropdownEntries;
    private int selectedDropdownEntry = -1;
    private boolean hasDropdown;

    private OnClickListener clickListener;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private DialogInterface.OnClickListener dropdownItemSelectedListener;
    private boolean hasSwitch;
    private boolean initializing = true;

    private int restoredChecked = -1;
    private int restoredSelectedIndex = -1;

    public MaterialItem(@NonNull Context context) {
        this(context, null);
    }
    public MaterialItem(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public MaterialItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr);init(context, attrs); }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_material_item, this, true);

        textContainer = findViewById(R.id.textContainer);
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        icon = findViewById(R.id.icon);
        chevron = findViewById(R.id.chevron);
        divider = findViewById(R.id.divider);
        switchWidget = findViewById(R.id.switchWidget);

        // Base padding to match Material 3 settings rows
        setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        textContainer.setMinimumHeight(dpToPx(48));

        // NOTE: This part wont be executed on runtime created widgets
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialItem);

            String t = a.getString(R.styleable.MaterialItem_itemTitle);
            String s = a.getString(R.styleable.MaterialItem_itemSubtitle);
            int iconRes = a.getResourceId(R.styleable.MaterialItem_itemIcon, 0);

            boolean attachMasterListener = a.getBoolean(R.styleable.MaterialItem_attachMasterListener, true);
            boolean showSwitch = a.getBoolean(R.styleable.MaterialItem_showSwitch, false);
            boolean checked = a.getBoolean(R.styleable.MaterialItem_isChecked, false);
            int dropdownEntriesRes = a.getResourceId(R.styleable.MaterialItem_dropdownEntries, 0);

            setTitle(t);
            setSubtitle(s);
            setIconResource(iconRes);

            setAttachMasterListener(attachMasterListener);
            setShowSwitch(showSwitch);
            if (hasSwitch) {
                setCheckedInternal(checked);
            }
            setDropdownEntries(dropdownEntriesRes != 0 ? context.getResources().getTextArray(dropdownEntriesRes) : null);
            setShowChevron(a.getBoolean(R.styleable.MaterialItem_showChevron, false));

            a.recycle();
        }

        initializing = false;
        post(this::refreshElements);
    }

    private void refreshElements() {
        if (initializing) return;
        boolean chevronValue = getChevronValue();
        boolean hasIcon = icon.getVisibility() != View.GONE;
        var textContainerLayoutParams = textContainer.getLayoutParams();

        ((MarginLayoutParams)textContainerLayoutParams).setMarginStart(hasIcon ? dpToPx(24) : 0);
        ((LayoutParams)textContainerLayoutParams).startToEnd = hasIcon ? icon.getId() : LayoutParams.UNSET;
        ((LayoutParams)textContainerLayoutParams).startToStart = hasIcon ? LayoutParams.UNSET : LayoutParams.PARENT_ID;
        textContainer.setLayoutParams(textContainerLayoutParams);

        dropdownSanityCheck();

        chevron.setVisibility(chevronValue ? View.VISIBLE : View.GONE);
        divider.setVisibility(chevronValue ? View.VISIBLE : View.GONE);

        refreshSwitch();
    }

    private void dropdownSanityCheck() {
        if (!hasDropdown) return;
        boolean chevronValue = getChevronValue();
        if (!masterListenerEnabled) {
            throw new IllegalStateException("Dropdown requires attachMasterListener to handle clicks");
        }
        if (hasSwitch && !chevronValue) {
            throw new IllegalStateException("Can't have dropdown and switch at the same time, either disable one or enable chevron to handle both.");
        }
    }

    private void refreshSwitch() {
        switchWidget.setVisibility(hasSwitch ? View.VISIBLE : View.GONE);
        if (!hasSwitch) return;

        boolean chevronValue = getChevronValue();
        switchWidget.setClickable(!masterListenerEnabled || chevronValue);
        switchWidget.setFocusable(!masterListenerEnabled || chevronValue);
        switchWidget.setDuplicateParentStateEnabled(chevronValue);

        switchWidget.setOnCheckedChangeListener((btn, isChecked) -> {
            if (hasSwitch && checkedChangeListener != null) checkedChangeListener.onCheckedChanged(btn, isChecked);
        });
        if (masterListenerEnabled && !chevronValue) {
            switchWidget.setBackground(null);
        }
    }

    private boolean getChevronValue() {
        return hasChevron && masterListenerEnabled && hasSwitch;
    }

    private void attachMasterListener() {
        setOnClickListener(v -> {
            if (!masterListenerEnabled) return;
            boolean setClicked = false;
            if (getChevronValue() && !hasDropdown) {
                setClicked = true;
            } else if (hasDropdown) {
                showDropdown();
            } else if (hasSwitch) {
                switchWidget.setChecked(!switchWidget.isChecked());
            } else {
                setClicked = true;
            }

            if (setClicked && clickListener != null) {
                clickListener.onClick(this);
            }
        });
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        applySelectableBackground(this, clickable);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        if (!masterListenerEnabled) {
            super.setOnClickListener(l);
        } else {
            clickListener = l;
        }
    }

    public void setOnCheckedChangeListener(@Nullable CompoundButton.OnCheckedChangeListener l) {
        requiresSwitch();
        checkedChangeListener = l;
    }

    public void setOnDropdownItemSelectedListener(@Nullable DialogInterface.OnClickListener l) {
        requiresDropdown();
        dropdownItemSelectedListener = l;
    }

    private void applySelectableBackground(View view, boolean enable) {
        if (!enable) {
            view.setBackgroundResource(0);
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
                .setTitle(title.getText())
                .setSingleChoiceItems(dropdownEntries, selectedDropdownEntry, (dialog, which) -> {
                    setSelectedItemInternal(which, false);
                    dialog.dismiss();
                })
                .show();
    }

    private void setSelectedItemInternal(int which, boolean forced) {
        if (!forced && selectedDropdownEntry == which) return;
        CharSequence value = dropdownEntries[which];
        selectedDropdownEntry = which;
        setSubtitle(value);
        if (!forced && dropdownItemSelectedListener != null) {
            dropdownItemSelectedListener.onClick(null, which);
        }
    }

    private void setCheckedInternal(boolean checked) {
        switchWidget.setChecked(checked);
    }

    private void requiresSwitch() {
        if (!hasSwitch) {
            throw new IllegalStateException("This item has no switch");
        }
    }

    private void requiresDropdown() {
        if (!hasDropdown) {
            throw new IllegalStateException("This item has no dropdown");
        }
    }

    public void setTitle(CharSequence text) { title.setText(text); }
    public CharSequence getText() {
        return title.getText();
    }
    public void setSubtitle(CharSequence text) {
        subtitle.setText(text);
        subtitle.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }
    public CharSequence setSubtitle() {
        return subtitle.getText();
    }

    public void setChecked(boolean checked) {
        requiresSwitch();
        setCheckedInternal(checked);
    }

    public boolean isChecked() {
        requiresSwitch();
        return switchWidget.isChecked();
    }

    public void setDropdownEntries(CharSequence[] entries) {
        boolean enable = entries != null && entries.length > 0;
        if (!hasDropdown && !enable) return;
        hasDropdown = enable;
        try {
            dropdownSanityCheck();
        } catch (Throwable e) {
            hasDropdown = false;
            throw e;
        }
        this.dropdownEntries = entries;
        post(this::refreshElements);
        if (!hasDropdown) return;
        setSelectedItemInternal(restoredSelectedIndex > -1 ? restoredSelectedIndex : 0, true);
        if (restoredSelectedIndex > -1) restoredSelectedIndex = -1;
    }
    public CharSequence[] getDropdownEntries() {
        requiresDropdown();
        return this.dropdownEntries;
    }
    public int getSelectedItemIndex() {
        requiresDropdown();
        return selectedDropdownEntry;
    }
    public CharSequence getSelectedItem() {
        requiresDropdown();
        return dropdownEntries[selectedDropdownEntry];
    }

    public void setSelectedItem(int index) {
        requiresDropdown();
        setSelectedItemInternal(index, false);
    }

    public void setIconResource(int iconRes) {
        icon.setImageResource(iconRes);
        icon.setVisibility(iconRes != 0 ? View.VISIBLE : View.GONE);
        post(this::refreshElements);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public void setShowSwitch(boolean enable) {
        if (hasSwitch == enable) return;
        hasSwitch = enable;
        post(this::refreshElements);
        if (hasSwitch && restoredChecked > -1) {
            setCheckedInternal(restoredChecked == 1);
            restoredChecked = -1;
        }
    }

    public void setShowChevron(boolean enable) {
        if (hasChevron == enable) return;
        hasChevron = enable;
        post(this::refreshElements);
    }

    public void setAttachMasterListener(boolean enable) {
        if (masterListenerEnabled == enable) return;
        // Old status, if enabled, do not touch it as it will register master listener as normal listener
        if (!masterListenerEnabled) {
            attachMasterListener();
        }

        masterListenerEnabled = enable;

        // No need for listener detaching as it has check guards and acts as no-op

        setClickable(masterListenerEnabled);
        setFocusable(masterListenerEnabled);

        post(this::refreshElements);
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
        if (hasSwitch) {
            ss.checked = this.isChecked() ? 1 : 0;
        }
        if (hasDropdown) {
            ss.selectedItemIndex = selectedDropdownEntry;
        }
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

        if (ss.checked == 0 || ss.checked == 1) {
            if (hasSwitch) {
                setCheckedInternal(ss.checked == 1);
            } else {
                restoredChecked = ss.checked;
            }
        }
        if (ss.selectedItemIndex > -1) {
            if (hasDropdown) {
                setSelectedItemInternal(ss.selectedItemIndex, true);
            } else {
                restoredSelectedIndex = ss.selectedItemIndex;
            }
        }
    }

    static class SavedState extends BaseSavedState {
        int checked = -1;
        int selectedItemIndex = -1;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = in.readInt();
            selectedItemIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(checked);
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
