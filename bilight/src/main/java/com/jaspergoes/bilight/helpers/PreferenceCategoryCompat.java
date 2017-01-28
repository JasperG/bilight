package com.jaspergoes.bilight.helpers;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jaspergoes.bilight.R;

public class PreferenceCategoryCompat extends PreferenceCategory {

    public PreferenceCategoryCompat(Context context) {
        super(context);
    }

    public PreferenceCategoryCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceCategoryCompat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        View view = super.onCreateView(parent);

        /* Primary color foreground */
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(parent.getResources().getColor(R.color.colorPrimary));

        return view;

    }

}