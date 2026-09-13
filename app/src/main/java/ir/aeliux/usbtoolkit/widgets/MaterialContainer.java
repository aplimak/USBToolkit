package ir.aeliux.usbtoolkit.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import ir.aeliux.usbtoolkit.R;

public class MaterialContainer extends LinearLayout {

    private LinearLayout contentContainer;
    private TextView headerView;
    private boolean isInflatingInternal;

    public MaterialContainer(Context context) {
        this(context, null);
    }

    public MaterialContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);

        isInflatingInternal = true;
        LayoutInflater.from(context)
                .inflate(R.layout.view_material_container, this, true);
        isInflatingInternal = false;

        headerView = findViewById(R.id.header);
        contentContainer = findViewById(R.id.content);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.MaterialContainer);
            String headerText = a.getString(
                    R.styleable.MaterialContainer_headerText);
            a.recycle();

            if (headerText != null && !headerText.isEmpty()) {
                setHeaderText(headerText);
            }
        }
    }

    /**
     * Redirect any child added to this container (from XML or code)
     * into the inner content LinearLayout, so it gets the card
     * background, padding and dividers for free.
     */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (isInflatingInternal || contentContainer == null) {
            // These are our internal views (header + card) — keep them here
            super.addView(child, index, params);
        } else {
            // These are the user's children — forward them into the card
            contentContainer.addView(child, index, params);
        }
    }

    public void setHeaderText(CharSequence text) {
        if (text == null || text.length() == 0) {
            headerView.setVisibility(GONE);
        } else {
            headerView.setText(text);
            headerView.setVisibility(VISIBLE);
        }
    }

    /** Escape hatch — if a caller needs direct access. */
    public LinearLayout getContentContainer() {
        return contentContainer;
    }
}