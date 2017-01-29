package com.jaspergoes.bilight.milight;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jaspergoes.bilight.helpers.Constants;
import com.jaspergoes.bilight.milight.objects.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

public class Controller {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /* Socket, used entire app session long */
    private static DatagramSocket socket;

    /* Local port to bind to - declared final to help compiler inline port number */
    private static final int localPort = 52123;

    /* Session bytes */
    private static byte milightSessionByte1;
    private static byte milightSessionByte2;

    /* Password bytes */
    private static byte milightPasswordByte1;
    private static byte milightPasswordByte2;

    /* Incremental value for each frame sent ( 0 - 255 ) */
    private static int noOnce = 2;

    /* Static instance */
    public static Controller INSTANCE;

    /* Controller device addr */
    public static InetAddress milightAddress;

    /* Port to use, in case of remote access */
    public static int milightPort = 5987;

    /* Controller device port - declared final to help compiler inline port number (I expect this port number to never change for v6 devices) */
    public static final int defaultMilightPort = 5987;

    /* List of all found wifi bridges */
    public static ArrayList<Device> milightDevices = new ArrayList<Device>();

    /* Whether we are connecting */
    public volatile static boolean isConnecting;

    /* Whether we are connected */
    private volatile static boolean isConnected;

    /* Timestamp of last command sent; Used to detect thread death */
    public static long keepAliveTime = 0;

    /* Whether the lamps are in WHITE mode */
    public static boolean nowWhite = false;

    /* New color, brightness and saturation values to be submitted */
    public volatile static int newColor = -1;
    public volatile static int newBrightness = -1;
    public volatile static int newSaturation = -1;
    public volatile static int newTemperature = -1;

    /* Last color, brightness and saturation values sent out */
    private static int lastColor = Integer.MAX_VALUE;
    private static int lastBrightness = Integer.MAX_VALUE;
    private static int lastSaturation = Integer.MAX_VALUE;
    private static int lastTemperature = Integer.MAX_VALUE;

    /* Name of the network interface in use */
    public volatile static String networkInterfaceName = "";

    /* The user-selection of devices and zones to be controlling */
    public static int[] controlDevices = new int[]{8, 7, 0};
    public static int[] controlZones = new int[]{0};

    public Controller() {
        INSTANCE = this;
    }

    public void discoverNetworks(final Context context) {

        networkInterfaceName = "";
        milightDevices.clear();
        isConnecting = true;

        int triedInterfaces = 0;

        try {

            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {

				/* Skip loopback and disabled interfaces */
                if (networkInterface.isLoopback() || !networkInterface.isUp())
                    continue;

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {

                    InetAddress localAddress = interfaceAddress.getAddress();
                    InetAddress broadcastAddress = interfaceAddress.getBroadcast();

					/* Skip any non-Inet4Address, and skip any interface without broadcast address */
                    if (!localAddress.getClass().getSimpleName().equals("Inet4Address") || broadcastAddress == null)
                        continue;

                    //networkInterfaceBound = false;
                    networkInterfaceName = networkInterface.getDisplayName() + " ( " + localAddress.getHostAddress() + " )";
                    context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

                    triedInterfaces++;

                    discoverDevices(localAddress, broadcastAddress, context);

                    if (milightDevices.size() > 0) {

                        isConnecting = false;
                        context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

                        return;

                    }

                }

            }

        } catch (SocketException e) {

        }

        /* Nothing found */
        isConnecting = false;
        context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

        if (triedInterfaces == 0) {
            Log.e("BILIGHT", "No suitable IPv4 network interfaces found for discovery cycle.");
            //System.exit(0);
        } else if (milightDevices.size() == 0) {
            //if (socket != null) socket.close();

            //ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("ibox.png")));
            //JOptionPane.showMessageDialog(null, \n\nNote: You need a WiFi iBox1 ( v6 ) ( aka. \"WiFi Bridge v6\" ) from Controller / Applamp / LimitlessLED\nconnected to your network to use this application.\n\nRestart the application to retry discovery.\n\nTerminating.", WindowHelper.appTitle, JOptionPane.ERROR_MESSAGE, icon);
            Log.e("BILIGHT", "No WiFi iBox1 ( v6 ) could be found on your network, or, the device did not respond within 15 seconds.");
            //System.exit(0);

        }

    }

    private void discoverDevices(InetAddress localAddress, InetAddress broadcastAddress, Context context) {

		/* Close any previously opened socket */
        if (socket != null) socket.close();

		/* Bind new socket to given localAddress - thus - attaching to specific interface */
        try {
            socket = new DatagramSocket(localPort, localAddress);
        } catch (SocketException e) {
            //JOptionPane.showMessageDialog(null, "Could not bind to port " + Integer.toString(localPort) + " at " + localAddress.getHostAddress() + ".\nIs another instance of the application already running?\n\n" + e.toString() + "\n\nTerminating.", WindowHelper.appTitle, JOptionPane.ERROR_MESSAGE);
            Log.e("BILIGHT", "Could not bind to port " + Integer.toString(localPort) + " at " + localAddress.getHostAddress() + ".");
            //System.exit(0);
        }

        //networkInterfaceBound = true;

        byte[] payload = new byte[]{(byte) 72, (byte) 70, (byte) 45, (byte) 65, (byte) 49, (byte) 49, (byte) 65, (byte) 83, (byte) 83, (byte) 73, (byte) 83, (byte) 84, (byte) 72, (byte) 82, (byte) 69, (byte) 65, (byte) 68};

        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, 64);

        int attempts = 0;

        do {

            try {
                socket.send(new DatagramPacket(payload, payload.length, broadcastAddress, 48899));
            } catch (IOException e) {
                /* This should never happen */
            }

			/* Note:
             * We'll spend eight seconds waiting for the first device to answer.
			 * We'll spend no more than four seconds once the first device has been found.
			 * If there's been no answer after eight seconds, we'll retry the whole thing, once.
			 * Max 16 seconds for discovery, or, show an error dialog afterwards. */

			/* Keep listening for incoming packets for at least four seconds, or eight while no devices are found */
            long timeout = System.currentTimeMillis();

            discoveryLoop:
            do {

                try {
                    socket.setSoTimeout(1000);

                    socket.receive(packet);

					/* We could have received literally anything from literally anywhere, so, check if this packet is actually something we're interested in */
                    String[] discovery = new String(buffer, 0, packet.getLength(), Charset.forName("UTF-8")).split(",");

					/* Keep it simple stupid. What is the chance, another device would present us with an UTF-8
                     * formatted string, with two ',' chars and a 12 char String after it's first comma?
					 * Close to zero, I suppose. So, this check will do. */
                    /* Also: discovery[2] always contains 'HF-LPB100' as in 'Low Power WiFi Module HF-LPB100' */
                    /* title (3rd returned value) is always 'HF-LPB100' as 'Low Power WiFi Module HF-LPB100' */
                    if (discovery.length == 3 && discovery[1].length() == 12) {

						/* Avoid duplicate entries. */
                        for (int i = milightDevices.size() - 1; i >= 0; i--) {
                            if (milightDevices.get(i).addrIP.equals(discovery[0])) {
                                continue discoveryLoop;
                            }
                        }

                        milightDevices.add(new Device(discovery[0], discovery[1].replaceAll("(.{2})", "$1" + ':').substring(0, 17), defaultMilightPort, false));
                        Collections.sort(milightDevices);
                        context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

                    }
                } catch (IOException e) {
                }

            }
            while (System.currentTimeMillis() - timeout < (milightDevices.size() == 0 ? 8000 : 4000));

        }
        while (++attempts < 2 && milightDevices.size() == 0);

    }

    public void setDevice(String address, int port, boolean upnp, Context context) {

        isConnecting = true;

        /* Close any previously opened socket */
        if (socket != null) socket.close();

		/* Bind new socket to any address - assuming android handles this right */
        try {
            socket = new DatagramSocket(localPort);
        } catch (SocketException e) {
            //JOptionPane.showMessageDialog(null, "Could not bind to port " + Integer.toString(localPort) + " at " + localAddress.getHostAddress() + ".\nIs another instance of the application already running?\n\n" + e.toString() + "\n\nTerminating.", WindowHelper.appTitle, JOptionPane.ERROR_MESSAGE);
            Log.e("BILIGHT", "Could not bind to port " + Integer.toString(localPort) + ".");
            //System.exit(0);
        }

        context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

        if (isConnected) {

            isConnected = false;

            synchronized (Controller.INSTANCE) {
                Controller.INSTANCE.notify();
            }

            // Wait 0.2 seconds for old packets to propagate
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }

        }

        try {
            milightAddress = InetAddress.getByName(address);
            milightPort = port;
        } catch (UnknownHostException e) {
            Log.e("BILIGHT", "INVALID HOST");
            isConnecting = false;
            context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));
            return;
        }

        byte[] payload = new byte[]{(byte) 32, (byte) 0, (byte) 0, (byte) 0, (byte) 22, (byte) 2, (byte) 98, (byte) 58, (byte) 213, (byte) 237, (byte) 163, (byte) 1, (byte) 174, (byte) 8, (byte) 45, (byte) 70, (byte) 97, (byte) 65, (byte) 167, (byte) 246, (byte) 220, (byte) 175, (byte) 211, (byte) 230, (byte) 0, (byte) 0, (byte) 30};

        try {
            socket.send(new DatagramPacket(payload, 27, milightAddress, milightPort));
        } catch (IOException e) {
        }

        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, 64);

		/* Keep listening for incoming packets for ten seconds */
        long timeout = System.currentTimeMillis() + 10000;

        do {

            try {
                socket.setSoTimeout(1000);

                socket.receive(packet);

				/* Check if the packet came from the selected device, and the received response is as expected */
                if (packet.getAddress().equals(milightAddress) && bytesToHex(buffer, packet.getLength()).indexOf("2800000011") == 0) {

                    String hostAddr = milightAddress.getHostAddress();

                    /* Check if this is a local device */
                    boolean found = false;
                    for (int i = 0; i < milightDevices.size(); i++) {
                        if (milightDevices.get(i).addrIP.equals(hostAddr)) {
                            found = true;
                            break;
                        }
                    }

                    /* Remote device */
                    if (!found) {

                        String hostMac = bytesToHex(buffer, packet.getLength()).substring(14, 14 + 12).replaceAll("(.{2})", "$1" + ':').substring(0, 17);
                        boolean changed = false;

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        try {

                            JSONArray remoteArray = new JSONArray(prefs.getString("remotes", "[]"));
                            for (int i = 0; i < remoteArray.length(); i++) {
                                JSONObject remote = remoteArray.getJSONObject(i);
                                if (remote.getString("n").equals(hostAddr) && remote.optInt("p", -1) == milightPort) {

                                    if (!remote.optString("m", "").equals(hostMac)) {
                                        remote.put("m", hostMac);
                                        remoteArray.put(i, remote);
                                        changed = true;
                                    }

                                    found = true;
                                    break;

                                }
                            }

                            if (!found) {

                                JSONObject n = new JSONObject();
                                n.put("n", hostAddr);
                                n.put("m", hostMac);
                                n.put("p", milightPort);
                                n.put("u", upnp);
                                remoteArray.put(n);

                                changed = true;

                            }

                            if (changed) {

                                prefs.edit().putString("remotes", remoteArray.toString()).apply();

                            }

                        } catch (JSONException e) {

                            prefs.edit().putString("remotes", "[]").apply();

                        }

                    }

                    milightSessionByte1 = buffer[19];
                    milightSessionByte2 = buffer[20];

					/* Discover password bytes before setting isConnected to true */
                    passwordDiscovery();

                    isConnected = true;
                    isConnecting = false;

                    context.sendBroadcast(new Intent(Constants.BILIGHT_DEVICE_CONNECTED));
                }

            } catch (IOException e) {
            }

        }
        while (!isConnected && timeout - System.currentTimeMillis() > 0);

        if (!isConnected) {

            //socket.close();

            //ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("ibox.png")));
            //JOptionPane.showMessageDialog(null, "Could not establish a connection to the WiFi iBox1 ( v6 ) on your network within 10 seconds.\n\nRestart the application to retry.\n\nTerminating.", WindowHelper.appTitle, JOptionPane.ERROR_MESSAGE, icon);
            Log.e("BILIGHT", "Could not establish a connection to the WiFi iBox1 ( v6 ) on your network within 10 seconds.");
            // System.exit(0);

            isConnecting = false;
            context.sendBroadcast(new Intent(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED));

        } else {
            /* Start keepalive thread */
            startWorkerThread();
        }

    }

    private void passwordDiscovery() {

		/* Set an incorrect pair of password-bytes, so we'll receive a packet with the correct password-bytes as response */
        milightPasswordByte1 = -1;
        milightPasswordByte2 = -1;

        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, 64);

        try {

            int sendAttempts = 0;

            do {

				/* Send a useless color payload with an incorrect set of password-bytes */
                socket.send(new DatagramPacket(buildColorPayload(0, 0), 22, milightAddress, milightPort));

				/* The second packet received will contain the correct password bytes */
                int receiveAttempts = 0;

                do {

                    try {

                        socket.receive(packet);

                        if (packet.getAddress().equals(milightAddress)) {

                            String response = bytesToHex(buffer, packet.getLength());

                            if (response.indexOf("8000000015") == 0 || response.indexOf("8000000021") == 0) {

                                milightPasswordByte1 = buffer[16];
                                milightPasswordByte2 = buffer[17];

                                return;

                            }

                        } else {
                            receiveAttempts--;
                        }

                    } catch (SocketTimeoutException e) {
                    }

                }
                while (++receiveAttempts < 2);

            }
            while (++sendAttempts < 2);

        } catch (IOException e) {
            /* This should never happen */
        }

        System.out.println("Could not retrieve password bytes.");

    }

    private void startWorkerThread() {

        new Thread() {

            @Override
            public void run() {

                boolean keepAlive = false;

                long wait;

                int[] controlDevices;
                int[] controlZones;

                ArrayList<byte[]> payloads = new ArrayList<byte[]>();

                while (isConnected) {

                    while (isConnected) {

                        controlDevices = Controller.controlDevices;
                        controlZones = Controller.controlZones;

                        if (lastColor != newColor && newColor != -1) {

                            lastColor = newColor;

                            if (nowWhite) {
                                nowWhite = false;
                                byte[] payload;
                                for (int i : controlDevices) {
                                    if (i == 0) {
                                        payload = buildColorPayload(i, 0);
                                        payloads.add(payload);
                                        payloads.add(payload);
                                    } else {
                                        for (int x : controlZones) {
                                            payload = buildColorPayload(i, x);
                                            payloads.add(payload);
                                            payloads.add(payload);
                                        }
                                    }
                                }
                            } else {
                                for (int i : controlDevices) {
                                    if (i == 0) {
                                        payloads.add(buildColorPayload(i, 0));
                                    } else {
                                        for (int x : controlZones) {
                                            payloads.add(buildColorPayload(i, x));
                                        }
                                    }
                                }
                            }
                        }

                        if (lastBrightness != newBrightness && newBrightness != -1) {

                            lastBrightness = newBrightness;

                            for (int i : controlDevices) {
                                if (i == 0) {
                                    payloads.add(buildBrightnessPayload(i, 0));
                                } else {
                                    for (int x : controlZones) {
                                        payloads.add(buildBrightnessPayload(i, x));
                                    }
                                }
                            }

                        }

                        if (lastSaturation != newSaturation && newSaturation != -1) {

                            lastSaturation = newSaturation;

                            for (int x : controlZones) {
                                payloads.add(buildSaturationPayload(x));
                            }

                        }

                        if (lastTemperature != newTemperature && newTemperature != -1) {

                            lastTemperature = newTemperature;

                            if (!nowWhite) {

                                setWhite();

                            } else {

                                for (int x : controlZones) {
                                    payloads.add(buildWhitePayload(8, x));
                                }

                            }

                        }

                        if (payloads.size() > 0) {

                            sendFrames(payloads, 75);
                            payloads.clear();

                            keepAliveTime = System.currentTimeMillis() + 5000;
                            keepAlive = false;

                        } else break;

                    }

                    if (isConnected) {

                        if (keepAlive) {

                            /* Send keep-alive packet */
                            try {

                                socket.send(new DatagramPacket(new byte[]{(byte) 208, (byte) 0, (byte) 0, (byte) 0, (byte) 2, milightSessionByte1, milightSessionByte2}, 7, milightAddress, milightPort));

                                keepAliveTime = System.currentTimeMillis() + 5000;

                            } catch (IOException e) {
                            /* This should never happen */
                            }

                        }

                    } else break;

                    if ((wait = keepAliveTime - System.currentTimeMillis()) > 0) {

                        try {

                            synchronized (Controller.INSTANCE) {

                                Controller.INSTANCE.wait(wait);

                            }

                        } catch (InterruptedException e) {

                        }

                    }

					/* Came out of wait, check if 5 seconds have passed */
                    keepAlive = keepAliveTime - System.currentTimeMillis() <= 0;

                }

            }

        }.start();

    }

    private byte[] buildColorPayload(int deviceGroup, int milightZone) {
        /* Each type of bulb has a different offset for the same position in the color spectrum.
         * List might be incomplete or (slightly) inaccurate. Improvements? Please pull.
		 *
		 * Full RED values:
		 *	group 0: 0
		 *	group 8: 10
		 *	group 7: 26
		*/
        int color = -128 + ((newColor + (deviceGroup == 7 ? 26 : (deviceGroup == 8 ? 10 : 0))) % 256);

		/* payload[4]; Length byte (Total amount of bytes - 5 header bytes) */
        /* payload[10]; First byte of lamp command (0x31) */
        /* payload[11] and payload[12]; Password bytes. */
        /* Rest should be self-explanatory */

        byte[] payload = new byte[]{(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 17, milightSessionByte1, milightSessionByte2, (byte) 0, (byte) (-128 + noOnce), (byte) 0, (byte) 49, milightPasswordByte1, milightPasswordByte2, (byte) deviceGroup, (byte) 1, (byte) color, (byte) color, (byte) color, (byte) color, (byte) milightZone, (byte) 0, (byte) 0};

		/* Checksum */
        payload[21] = (byte) ((char) (0xFF & payload[10]) + (char) (0xFF & payload[11]) + (char) (0xFF & payload[12]) + (char) (0xFF & payload[13]) + (char) (0xFF & payload[14]) + (char) (0xFF & payload[15]) + (char) (0xFF & payload[16]) + (char) (0xFF & payload[17]) + (char) (0xFF & payload[18]) + (char) (0xFF & payload[19]) + (char) (0xFF & payload[20]));

		/* Increment sequential number */
        noOnce = (noOnce + 1) % 256;

        return payload;
    }

    private byte[] buildBrightnessPayload(int deviceGroup, int milightZone) {
        /* Note: We intentionally take the brightness value from newBrightness here.
         * We do not nescessarily want to set all lamps to the *SAME* brightness,
		 * we just want to set it to the very, very LATEST calculated brightness.
		 */

		/* Always set wifi bridge brightness to 100? Nice for me personally,
         * since I've placed my iBox right behind my TV. As others might. Whatever. */

        byte[] payload = new byte[]{(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 17, milightSessionByte1, milightSessionByte2, (byte) 0, (byte) (-128 + noOnce), (byte) 0, (byte) 49, milightPasswordByte1, milightPasswordByte2, (byte) deviceGroup, (byte) (deviceGroup == 8 ? 3 : 2), (byte) newBrightness, (byte) 0, (byte) 0, (byte) 0, (byte) milightZone, (byte) 0, (byte) 0};

		/* Checksum */
        payload[21] = (byte) ((char) (0xFF & payload[10]) + (char) (0xFF & payload[11]) + (char) (0xFF & payload[12]) + (char) (0xFF & payload[13]) + (char) (0xFF & payload[14]) + (char) (0xFF & payload[15]) + (char) (0xFF & payload[16]) + (char) (0xFF & payload[17]) + (char) (0xFF & payload[18]) + (char) (0xFF & payload[19]) + (char) (0xFF & payload[20]));

		/* Increment sequential number */
        noOnce = (noOnce + 1) % 256;

        return payload;
    }

    private byte[] buildSaturationPayload(int milightZone) {
        /* Note: This only works for deviceGroup 8; Lamps with saturation control. RGBWW */
        byte[] payload = new byte[]{(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 17, milightSessionByte1, milightSessionByte2, (byte) 0, (byte) (-128 + noOnce), (byte) 0, (byte) 49, milightPasswordByte1, milightPasswordByte2, (byte) 8, (byte) 2, (byte) newSaturation, (byte) 0, (byte) 0, (byte) 0, (byte) milightZone, (byte) 0, (byte) 0};

		/* Checksum */
        payload[21] = (byte) ((char) (0xFF & payload[10]) + (char) (0xFF & payload[11]) + (char) (0xFF & payload[12]) + (char) (0xFF & payload[13]) + (char) (0xFF & payload[14]) + (char) (0xFF & payload[15]) + (char) (0xFF & payload[16]) + (char) (0xFF & payload[17]) + (char) (0xFF & payload[18]) + (char) (0xFF & payload[19]) + (char) (0xFF & payload[20]));

		/* Increment sequential number */
        noOnce = (noOnce + 1) % 256;

        return payload;
    }

    public void setWhite() {

        /* We'll just send each command for white two times */

        nowWhite = true;

        int[] controlDevices = Controller.controlDevices;
        int[] controlZones = Controller.controlZones;

        ArrayList<byte[]> payloads = new ArrayList<byte[]>();
        byte[] payload;

        for (int i : controlDevices) {
            if (i == 0) {
                payload = buildWhitePayload(i, 0);
                payloads.add(payload);
                payloads.add(payload);
            } else {
                for (int x : controlZones) {
                    payload = buildWhitePayload(i, x);
                    payloads.add(payload);
                    payloads.add(payload);
                }
            }
        }

        sendFrames(payloads, 100);

    }

    private byte[] buildWhitePayload(int group, int milightZone) {

        byte[] payload = new byte[]{(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 17, milightSessionByte1, milightSessionByte2, (byte) 0, (byte) (-128 + noOnce), (byte) 0, (byte) 49, milightPasswordByte1, milightPasswordByte2, (byte) group, (byte) (group == 8 ? 5 : 3), (byte) (group == 8 ? (newTemperature != -1 ? newTemperature : 65) : 5), (byte) 0, (byte) 0, (byte) 0, (byte) (group == 0 ? 0 : milightZone), (byte) 0, (byte) 0};

		/* Checksum */
        payload[21] = (byte) ((char) (0xFF & payload[10]) + (char) (0xFF & payload[11]) + (char) (0xFF & payload[12]) + (char) (0xFF & payload[13]) + (char) (0xFF & payload[14]) + (char) (0xFF & payload[15]) + (char) (0xFF & payload[16]) + (char) (0xFF & payload[17]) + (char) (0xFF & payload[18]) + (char) (0xFF & payload[19]) + (char) (0xFF & payload[20]));

		/* Increment sequential number */
        noOnce = (noOnce + 1) % 256;

        return payload;

    }

    public void setOnOff(boolean onOff) {

        /* We'll just send each command for on/off two times */

        int[] controlDevices = Controller.controlDevices;
        int[] controlZones = Controller.controlZones;

        ArrayList<byte[]> payloads = new ArrayList<byte[]>();
        byte[] payload;

        for (int i : controlDevices) {
            if (i == 0) {
                payload = buildSwitchPayload(i, 0, onOff);
                payloads.add(payload);
                payloads.add(payload);
            } else {
                for (int x : controlZones) {
                    payload = buildSwitchPayload(i, x, onOff);
                    payloads.add(payload);
                    payloads.add(payload);
                }
            }
        }

        sendFrames(payloads, 100);

    }

    private byte[] buildSwitchPayload(int group, int milightZone, boolean onOff) {

        byte[] payload = new byte[]{(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 17, milightSessionByte1, milightSessionByte2, (byte) 0, (byte) (-128 + noOnce), (byte) 0, (byte) 49, milightPasswordByte1, milightPasswordByte2, (byte) group, (byte) (group == 8 ? 4 : 3), (byte) (group == 0 ? (onOff ? 3 : 4) : (onOff ? 1 : 2)), (byte) 0, (byte) 0, (byte) 0, (byte) (group == 0 ? 0 : milightZone), (byte) 0, (byte) 0};

		/* Checksum */
        payload[21] = (byte) ((char) (0xFF & payload[10]) + (char) (0xFF & payload[11]) + (char) (0xFF & payload[12]) + (char) (0xFF & payload[13]) + (char) (0xFF & payload[14]) + (char) (0xFF & payload[15]) + (char) (0xFF & payload[16]) + (char) (0xFF & payload[17]) + (char) (0xFF & payload[18]) + (char) (0xFF & payload[19]) + (char) (0xFF & payload[20]));

		/* Increment sequential number */
        noOnce = (noOnce + 1) % 256;

        return payload;

    }

    public void refresh() {

        lastColor = Integer.MAX_VALUE;
        lastBrightness = Integer.MAX_VALUE;
        lastSaturation = Integer.MAX_VALUE;

        synchronized (Controller.INSTANCE) {
            Controller.INSTANCE.notify();
        }

    }

    private void sendFrames(ArrayList<byte[]> payloads, int sleep) {

        for (byte[] payload : payloads) {

            try {

                socket.send(new DatagramPacket(payload, 22, milightAddress, milightPort));

				/* Wait 'sleep' milliseconds for previous RF command to propagate from iBox */
                Thread.sleep(sleep);

            } catch (IOException e) {

                /* This should never happen, but, if it does, for whatever reason; There would be no need for
                 * a followup Thread.sleep call, which will not be performed once this catch block has been reached */

            } catch (InterruptedException e) {

                /* This may happen, and I wouldn't care one byte */

            }

        }

    }

    private static String bytesToHex(byte[] bytes, int length) {
        char[] hexChars = new char[length * 2];
        int j = 0, v;
        while (j < length) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            j++;
        }
        return new String(hexChars);
    }

}