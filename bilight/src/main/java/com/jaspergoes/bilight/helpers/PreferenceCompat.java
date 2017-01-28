package com.jaspergoes.bilight.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.jaspergoes.bilight.R;

public class PreferenceCompat extends Preference {

    private ImageView mIcon;
    private Drawable mIconDrawable;

    public PreferenceCompat(Context context) {
        super(context);
        init(context, null);
    }

    public PreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        PrefUtil.setLayoutResource(this, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mIcon = (ImageView) view.findViewById(R.id.preferenceIcon);
        if (mIconDrawable != null) {
            mIcon.setImageDrawable(mIconDrawable);
        }
    }

    @Override
    public void setIcon(Drawable icon) {
        mIconDrawable = icon;
        if (mIcon != null) {
            mIcon.setImageDrawable(icon);
        }
    }
}