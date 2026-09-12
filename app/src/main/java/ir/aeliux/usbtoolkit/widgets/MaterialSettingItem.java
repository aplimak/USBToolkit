package ir.aeliux.usbtoolkit.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.materialswitch.MaterialSwitch;

import ir.aeliux.usbtoolkit.R;

public class MaterialSettingItem extends ConstraintLayout {

    public interface OnSettingChangeListener {
        void onCheckedChanged(boolean isChecked);
        void onClicked();
        void onDropdownItemSelected(int index, String value);
    }

    private LinearLayout textContainer;
    private TextView title, subtitle, dropdownValue;
    private ImageView icon, chevron;
    private View divider;
    private MaterialSwitch switchWidget;
    private LinearLayout trailingContainer;

    private OnSettingChangeListener listener;
    private boolean isDropdown = false;
    private CharSequence[] dropdownEntries;

    // --- Constructors ---
    public MaterialSettingItem(@NonNull Context context) {
        this(context, null);
    }

    public MaterialSettingItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialSettingItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_material_setting_item, this, true);

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
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialSettingItem);

            String t = a.getString(R.styleable.MaterialSettingItem_settingTitle);
            String s = a.getString(R.styleable.MaterialSettingItem_settingSubtitle);
            int iconRes = a.getResourceId(R.styleable.MaterialSettingItem_settingIcon, 0);

            boolean showSwitch = a.getBoolean(R.styleable.MaterialSettingItem_showSwitch, false);
            boolean showChevron = a.getBoolean(R.styleable.MaterialSettingItem_showChevron, false);
            boolean clickableAttr = a.getBoolean(R.styleable.MaterialSettingItem_isClickable, false);
            boolean checked = a.getBoolean(R.styleable.MaterialSettingItem_isChecked, false);
            boolean dropdownAttr = a.getBoolean(R.styleable.MaterialSettingItem_isDropdown, false);
            int entriesRes = a.getResourceId(R.styleable.MaterialSettingItem_dropdownEntries, 0);

            if (t != null) title.setText(t);
            if (s != null) {
                subtitle.setText(s);
                subtitle.setVisibility(View.VISIBLE);
            }
            if (iconRes != 0) {
                icon.setImageResource(iconRes);
                icon.setVisibility(View.VISIBLE);
            }

            // Configure trailing elements
            if (showSwitch) {
                switchWidget.setVisibility(View.VISIBLE);
                switchWidget.setChecked(checked);
                divider.setVisibility(View.VISIBLE);
                switchWidget.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (listener != null) listener.onCheckedChanged(isChecked);
                });
            }

            if (showChevron) {
                chevron.setVisibility(View.VISIBLE);
            }

            if (dropdownAttr) {
                this.isDropdown = true;
                dropdownValue.setVisibility(View.VISIBLE);
                if (showChevron) chevron.setVisibility(View.VISIBLE);
                if (entriesRes != 0) {
                    dropdownEntries = context.getResources().getTextArray(entriesRes);
                }
                setupDropdown();
            }

            // Full-row clickable
            if (clickableAttr && !showSwitch && !dropdownAttr) {
                setClickable(true);
                setFocusable(true);
                applySelectableBackground();
                if (showChevron) chevron.setVisibility(View.VISIBLE);
                setOnClickListener(v -> {
                    if (listener != null) listener.onClicked();
                });
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

        setOnClickListener(v -> {
            if (switchWidget.getVisibility() == View.VISIBLE) {
                switchWidget.setChecked(!switchWidget.isChecked());
            } else if (!isDropdown && listener != null) {
                listener.onClicked();
            }
        });
    }

    private void applySelectableBackground() {
        TypedValue outValue = new TypedValue();
        boolean resolved = getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
        );
        if (resolved && outValue.resourceId != 0) {
            setBackgroundResource(outValue.resourceId);
        }
    }

    private void setupDropdown() {
        if (dropdownEntries == null || dropdownEntries.length == 0) return;

        // Set initial value
        dropdownValue.setText(dropdownEntries[0]);
        setClickable(true);
        setFocusable(true);

        setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(title.getText())
                    .setItems(dropdownEntries, (dialog, which) -> {
                        dropdownValue.setText(dropdownEntries[which]);
                        if (listener != null) {
                            listener.onDropdownItemSelected(which, dropdownEntries[which].toString());
                        }
                    })
                    .show();
        });
    }

    // --- Public API ---
    public void setOnSettingChangeListener(OnSettingChangeListener l) {
        this.listener = l;
    }

    public void setChecked(boolean checked) {
        switchWidget.setChecked(checked);
    }

    public boolean isChecked() {
        return switchWidget.isChecked();
    }

    public void setTitle(CharSequence text) { title.setText(text); }
    public void setSubtitle(CharSequence text) {
        subtitle.setText(text);
        subtitle.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }
    public void setDropdownEntries(CharSequence[] entries) {
        this.dropdownEntries = entries;
        setupDropdown();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
