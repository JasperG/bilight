package com.jaspergoes.bilight.helpers;

import android.content.res.XmlResourceParser;
import android.preference.Preference;
import android.util.AttributeSet;

import com.jaspergoes.bilight.R;

public class PrefUtil {

    public static void setLayoutResource(Preference preference, AttributeSet attrs) {
        boolean foundLayout = false;
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                final String namespace = ((XmlResourceParser) attrs).getAttributeNamespace(0);
                if (namespace.equals("http://schemas.android.com/apk/res/android") && attrs.getAttributeName(i).equals("layout")) {
                    foundLayout = true;
                    break;
                }
            }
        }
        if (!foundLayout)
            preference.setLayoutResource(R.layout.preference_custom);
    }

}
