package com.jaspergoes.bilight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jaspergoes.bilight.helpers.Constants;
import com.jaspergoes.bilight.helpers.PortMapper;
import com.jaspergoes.bilight.helpers.PreferenceActivityCompat;
import com.jaspergoes.bilight.helpers.PreferenceCategoryCompat;
import com.jaspergoes.bilight.helpers.PreferenceCompat;
import com.jaspergoes.bilight.milight.Controller;
import com.jaspergoes.bilight.milight.objects.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends PreferenceActivityCompat {

    private class DevicePreference extends PreferenceCompat {

        private final int deviceIndex;

        DevicePreference(Context context, int deviceIndex) {

            super(context);

            this.deviceIndex = deviceIndex;

            Device device = Controller.milightDevices.get(deviceIndex);

            this.setTitle(device.addrIP);
            this.setSummary(device.addrMAC + (device.addrPort != Controller.defaultMilightPort ? " ( " + getString(R.string.port) + ": " + device.addrPort + " )" : ""));

        }

        @Override
        protected void onClick() {

            if (this.isEnabled() && !Controller.isConnecting) {

                Controller.isConnecting = true;

                Device device = Controller.milightDevices.get(deviceIndex);

                final String addressIP = device.addrIP;
                final String addressMac = device.addrMAC;
                final int addressPort = device.addrPort;

                boolean hasRemoteConnectionConfigured = false;
                for (Device remoteDevice : remoteMilightDevices) {
                    if (addressMac.equals(remoteDevice.addrMAC)) {
                        hasRemoteConnectionConfigured = true;
                        break;
                    }
                }

                new Thread(hasRemoteConnectionConfigured ? new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setDevice(addressIP, addressPort, getApplicationContext());

                    }

                } : new Runnable() {

                    @Override
                    public void run() {

                        /* Try UPnP approach */
                        String outerIP = PortMapper.getExternalIP();
                        if (outerIP != null) {

                            int mappedPort = PortMapper.checkMapped(addressIP);
                            if (mappedPort == -1) {

                                mappedPort = 5000 + (int) (Math.random() * 1999);
                                if (PortMapper.mapPort("", mappedPort, addressIP, addressPort, "Milight iBox " + addressMac)) {

                                    /* Created an upnp mapping. Great. Connect via external, to add it to the list. */
                                    Controller.INSTANCE.setDevice(outerIP, mappedPort, getApplicationContext());

                                    return;

                                }

                            } else {

                                /* We already have a mapped port. Great. Connect via external, to add it to the list. */
                                Controller.INSTANCE.setDevice(outerIP, mappedPort, getApplicationContext());

                                return;

                            }

                        }

                        /* Fallback, could not create upnp mapping; Use local */
                        Controller.INSTANCE.setDevice(addressIP, addressPort, getApplicationContext());

                    }

                }).start();

                updateList();

            }

        }

    }

    private class RemoteDevicePreference extends PreferenceCompat implements View.OnLongClickListener {

        private final int deviceIndex;

        RemoteDevicePreference(Context context, int deviceIndex) {

            super(context);

            this.deviceIndex = deviceIndex;

            Device device = remoteMilightDevices.get(deviceIndex);

            this.setTitle(device.addrIP);
            this.setSummary(device.addrMAC + (device.addrPort != Controller.defaultMilightPort ? " ( " + getString(R.string.port) + ": " + device.addrPort + " )" : ""));

        }

        @Override
        protected void onClick() {

            if (this.isEnabled() && !Controller.isConnecting) {

                Device device = remoteMilightDevices.get(deviceIndex);

                final String addressIP = device.addrIP;
                final int addressPort = device.addrPort;

                Controller.isConnecting = true;

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setDevice(addressIP, addressPort, getApplicationContext());

                    }

                }).start();

                updateList();

            }

        }

        @Override
        public boolean onLongClick(View view) {

            if (this.isEnabled() && !Controller.isConnecting) {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                try {

                    JSONArray newArray = new JSONArray();
                    JSONArray remoteArray = new JSONArray(prefs.getString("remotes", "[]"));
                    for (int i = 0; i < remoteArray.length(); i++) {
                        if (i != deviceIndex) {
                            newArray.put(remoteArray.getJSONObject(i));
                        }
                    }
                    prefs.edit().putString("remotes", newArray.toString()).apply();

                } catch (JSONException e) {

                    prefs.edit().putString("remotes", "[]").apply();

                }

                updateList();

                return true;

            }

            return false;

        }

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED.equals(action)) {

                updateList();

            } else if (Constants.BILIGHT_DEVICE_CONNECTED.equals(action)) {

                startActivity(new Intent(context, ControlActivity.class));

            }

        }

    };

    private PreferenceCategoryCompat deviceList;
    private PreferenceCategoryCompat remoteList;
    private PreferenceCompat addRemote;

    public static ArrayList<Device> remoteMilightDevices = new ArrayList<Device>();

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle bundle) {

        super.onCreate(bundle);

        new Controller();

        new Thread(new Runnable() {

            @Override
            public void run() {
                Controller.INSTANCE.discoverNetworks(getApplicationContext());
            }

        }).start();

        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Local Device Category
        deviceList = new PreferenceCategoryCompat(this);
        root.addPreference(deviceList);

        // Remote Device Category
        remoteList = new PreferenceCategoryCompat(this);
        remoteList.setOrderingAsAdded(true);
        remoteList.setTitle(getString(R.string.remote_devices));
        root.addPreference(remoteList);

        addRemote = new PreferenceCompat(this);
        addRemote.setTitle(getString(R.string.add_remote_device));
        addRemote.setSummary(getString(R.string.add_remote_device_summary));
        addRemote.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                startActivity(new Intent(getApplicationContext(), AddRemoteActivity.class));

                return true;

            }

        });
        remoteList.addPreference(addRemote);

        setPreferenceScreen(root);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                ListView listView = (ListView) adapterView;
                ListAdapter listAdapter = listView.getAdapter();
                Object obj = listAdapter.getItem(position);
                if (obj != null && obj instanceof View.OnLongClickListener) {
                    View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                    return longListener.onLongClick(view);
                }
                return false;
            }
        });

    }

    @Override
    protected void onResume() {

        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED);
        filter.addAction(Constants.BILIGHT_DEVICE_CONNECTED);
        registerReceiver(receiver, filter);

        updateList();

    }

    @Override
    protected void onPause() {

        super.onPause();

        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
        }

    }

    void updateList() {

        boolean maySelect = !Controller.isConnecting;
        int i, l;

        findViewById(R.id.busy).setVisibility(maySelect ? View.GONE : View.VISIBLE);
        addRemote.setEnabled(maySelect);

        /* Remove all discovered devices from the PreferenceGroup */
        deviceList.removeAll();
        deviceList.setTitle(getString(R.string.local_devices) + (Controller.networkInterfaceName.length() > 0 ? " - " + Controller.networkInterfaceName : ""));

        /* Re-populate the list with discovered devices */
        if ((l = Controller.milightDevices.size()) > 0) {
            for (i = 0; i < l; i++) {
                DevicePreference pref = new DevicePreference(this, i);
                pref.setIcon(getResources().getDrawable(R.drawable.ic_bulb));
                pref.setEnabled(maySelect);
                deviceList.addPreference(pref);
            }
        } else {
            PreferenceCompat pref = new PreferenceCompat(this);
            pref.setTitle(getString(R.string.no_milight_devices_found));
            pref.setIcon(getResources().getDrawable(R.drawable.ic_bulb_grey));
            pref.setEnabled(false);
            deviceList.addPreference(pref);
        }

        /* Remove all remote devices from the PreferenceGroup */
        for (i = remoteList.getPreferenceCount() - 1; i > 0; i--) {
            remoteList.removePreference(remoteList.getPreference(i));
        }
        remoteMilightDevices.clear();

        /* Re-populate the list of remote devices from sharedpreferences */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            JSONArray remoteArray = new JSONArray(prefs.getString("remotes", "[]"));
            for (i = 0; i < remoteArray.length(); i++) {
                JSONObject remote = remoteArray.getJSONObject(i);
                remoteMilightDevices.add(new Device(remote.getString("n"), remote.getString("m"), remote.getInt("p")));
                RemoteDevicePreference pref = new RemoteDevicePreference(this, i);
                pref.setIcon(getResources().getDrawable(R.drawable.ic_bulb));
                pref.setEnabled(maySelect);
                remoteList.addPreference(pref);
            }
        } catch (JSONException e) {
            prefs.edit().putString("remotes", "[]").apply();
        }

    }

}