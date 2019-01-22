package com.vijay.jsonwizard.customviews;

import android.content.Context;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;

/***
 * <name> NumberSelectorSpinner</name> Custom numbers selector spinner to overcome
 * **/
public class NumberSelectorSpinner extends AppCompatSpinner {
    private OnItemSelectedListener listener;

    public NumberSelectorSpinner(Context context) {
        super(context);
    }

    public NumberSelectorSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberSelectorSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public NumberSelectorSpinner(Context context, int mode) {
        super(context, mode);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null) {
            listener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }

    public void setListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
}