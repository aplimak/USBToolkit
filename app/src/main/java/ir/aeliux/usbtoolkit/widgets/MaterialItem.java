package ir.aeliux.usbtoolkit.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private TextView title, subtitle, dropdownValue;
    private ImageView icon, chevron;
    private View divider;
    private MaterialSwitch switchWidget;
    private LinearLayout trailingContainer;

    private boolean hasChevron = false;
    private boolean masterListenerEnabled = false;
    private CharSequence[] dropdownEntries;
    private OnClickListener clickListener;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private DialogInterface.OnClickListener dropdownItemSelectedListener;

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
        dropdownValue = findViewById(R.id.dropdownValue);
        trailingContainer = findViewById(R.id.trailingContainer);

        // Base padding to match Material 3 settings rows
        setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        setMinimumHeight(dpToPx(72));

        // --- Read custom attributes ---
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialItem);

            String t = a.getString(R.styleable.MaterialItem_itemTitle);
            String s = a.getString(R.styleable.MaterialItem_itemSubtitle);
            int iconRes = a.getResourceId(R.styleable.MaterialItem_itemIcon, 0);

            boolean showSwitch = a.getBoolean(R.styleable.MaterialItem_showSwitch, false);
            boolean showDropdown = a.getBoolean(R.styleable.MaterialItem_showDropdown, false);
            hasChevron = (showSwitch || showDropdown) && a.getBoolean(R.styleable.MaterialItem_showChevron, false);
            boolean clickableAttr = a.getBoolean(R.styleable.MaterialItem_isClickable, true);
            boolean checked = showSwitch &&  a.getBoolean(R.styleable.MaterialItem_isChecked, false);
            int dropdownEntriesRes = showDropdown ? a.getResourceId(R.styleable.MaterialItem_dropdownEntries, 0) : 0;

            if (t != null) title.setText(t);
            if (s != null) {
                subtitle.setText(s);
                subtitle.setVisibility(View.VISIBLE);
            }
            if (iconRes != 0) {
                icon.setImageResource(iconRes);
                icon.setVisibility(View.VISIBLE);
            } else {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) textContainer.getLayoutParams();

                params.setMarginStart(0);
                textContainer.setLayoutParams(params);
            }

            // Configure trailing elements
            if (showSwitch) {
                switchWidget.setVisibility(View.VISIBLE);
                switchWidget.setClickable(!hasChevron);
                switchWidget.setFocusable(!hasChevron);
                switchWidget.setChecked(checked);
                switchWidget.setDuplicateParentStateEnabled(hasChevron);
                switchWidget.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (checkedChangeListener != null) checkedChangeListener.onCheckedChanged(btn, isChecked);
                });
                if (!hasChevron) {
                    switchWidget.setBackground(null);
                }
            }

            if (showDropdown) {
                dropdownValue.setVisibility(View.VISIBLE);
                if (dropdownEntriesRes != 0) {
                    dropdownEntries = context.getResources().getTextArray(dropdownEntriesRes);
                }
                setupDropdown();
            }

            // Full-row clickable
            if (clickableAttr) {
                setClickable(true);
                setFocusable(true);

                setOnClickListener(v -> {
                    boolean setClicked = false;
                    if (hasChevron) {
                        setClicked = true;
                    } else if (showSwitch) {
                        switchWidget.performClick();
                    } else if (showDropdown) {
                        dropdownValue.performClick();
                    } else {
                        setClicked = true;
                    }

                    if (setClicked && clickListener != null) {
                        clickListener.onClick(this);
                    }
                });
                masterListenerEnabled = true;
            }

            a.recycle();
        }

        // Adjust text container start margin if icon is hidden
        post(() -> {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) textContainer.getLayoutParams();
            if (icon.getVisibility() == View.GONE) {
                params.startToEnd = ConstraintLayout.LayoutParams.UNSET;
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                textContainer.setLayoutParams(params);
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

    private void setupDropdown() {
        if (dropdownEntries == null || dropdownEntries.length == 0) return;

        // Set initial value
        dropdownValue.setText(dropdownEntries[0]);

        dropdownValue.setClickable(!hasChevron);
        dropdownValue.setFocusable(!hasChevron);
        dropdownValue.setDuplicateParentStateEnabled(hasChevron);
        if (!hasChevron) {
            dropdownValue.setBackground(null);
        }

        dropdownValue.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(title.getText())
                    .setItems(dropdownEntries, (dialog, which) -> {
                        dropdownValue.setText(dropdownEntries[which]);
                        if (dropdownItemSelectedListener != null) {
                            dropdownItemSelectedListener.onClick(dialog, which);
                        }
                    })
                    .show();
        });
    }

    private void requiresSwitch() {
        if (switchWidget.getVisibility() != View.VISIBLE) {
            throw new IllegalStateException("This item has no switch");
        }
    }

    private void requiresDropdown() {
        if (dropdownValue.getVisibility() != View.VISIBLE) {
            throw new IllegalStateException("This item has no dropdown");
        }
    }

    public void setChecked(boolean checked) {
        requiresSwitch();
        switchWidget.setChecked(checked);
    }

    public boolean isChecked() {
        requiresSwitch();
        return switchWidget.isChecked();
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

    public void setDropdownEntries(CharSequence[] entries) {
        requiresDropdown();
        this.dropdownEntries = entries;
        setupDropdown();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
