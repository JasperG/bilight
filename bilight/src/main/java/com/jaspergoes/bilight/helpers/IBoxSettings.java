package com.jaspergoes.bilight.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.jaspergoes.bilight.milight.Controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IBoxSettings {

    private final String key;

    public boolean hasIBoxLamp;
    public boolean hasRGBWW;
    public boolean hasRGBW;
    public boolean hasDualW;

    public String title;

    public IBoxSettings(Context context, String key) {

        this.key = key;

        hasIBoxLamp = true;
        hasRGBWW = true;
        hasRGBW = true;
        hasDualW = true;

        title = "";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {

            JSONArray remoteArray = new JSONArray(prefs.getString("devices", "[]"));
            for (int i = 0; i < remoteArray.length(); i++) {

                JSONObject device = remoteArray.getJSONObject(i);
                if (device.getString("m").equals(key)) {
                    hasIBoxLamp = device.getBoolean("i");
                    hasRGBWW = device.getBoolean("d");
                    hasRGBW = device.getBoolean("w");
                    hasDualW = device.getBoolean("x");
                    title = device.optString("t", "");
                }
            }

        } catch (JSONException e) {

            prefs.edit().putString("devices", "[]").apply();

        }

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        if (mFirebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString("mac", key);
            params.putString("title", title);
            params.putString("has_lamp", hasIBoxLamp ? "yes" : "no");
            params.putString("has_rgbw", hasRGBW ? "yes" : "no");
            params.putString("has_rgbww", hasRGBWW ? "yes" : "no");
            params.putString("has_dualw", hasDualW ? "yes" : "no");
            mFirebaseAnalytics.logEvent("device_connect", params);
        }

        /* On load; Propagate to Controller */
        List<Integer> deviceList = new ArrayList<Integer>();

        if (hasRGBWW)
            deviceList.add(8);

        if (hasRGBW)
            deviceList.add(7);

        if (hasDualW)
            deviceList.add(3);

        if (hasIBoxLamp)
            deviceList.add(0);

        int ret[] = new int[deviceList.size()];
        for (int i = 0; i < ret.length; i++) ret[i] = deviceList.get(i);

        Controller.controlDevices = ret;

        if (hasRGBW || hasRGBWW || hasDualW) {

            Controller.controlZones = new int[]{0};

        } else {

            Controller.controlZones = new int[]{-1};

        }

    }

    public void save(Context context, String key) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {

            boolean found = false;

            JSONArray remoteArray = new JSONArray(prefs.getString("devices", "[]"));
            for (int i = 0; i < remoteArray.length(); i++) {

                JSONObject device = remoteArray.getJSONObject(i);
                if (device.getString("m").equals(key)) {

                    found = true;

                    device.put("i", hasIBoxLamp);
                    device.put("d", hasRGBWW);
                    device.put("w", hasRGBW);
                    device.put("x", hasDualW);
                    device.put("t", title);

                    remoteArray.put(i, device);

                    break;

                }
            }

            if (!found) {

                JSONObject device = new JSONObject();
                device.put("m", key);
                device.put("i", hasIBoxLamp);
                device.put("d", hasRGBWW);
                device.put("w", hasRGBW);
                device.put("x", hasDualW);
                device.put("t", title);
                remoteArray.put(device);

            }

            prefs.edit().putString("devices", remoteArray.toString()).apply();

        } catch (JSONException e) {

            prefs.edit().putString("devices", "[]").apply();

        }

    }

}
