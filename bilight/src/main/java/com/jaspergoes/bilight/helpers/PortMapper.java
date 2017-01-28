package com.jaspergoes.bilight.helpers;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;

import java.io.IOException;

public class PortMapper {

    private static final int SCAN_TIMEOUT = 5000;
    private static final String UDP_PROTOCOL = "UDP";

    private static InternetGatewayDevice[] internetGatewayDevices;

    public static String getExternalIP() {

        try {

            internetGatewayDevices = InternetGatewayDevice.getDevices(SCAN_TIMEOUT);

            if (internetGatewayDevices != null) {

                for (InternetGatewayDevice IGD : internetGatewayDevices) {

                    try {

                        return IGD.getExternalIPAddress().toString();

                    } catch (UPNPResponseException e) {

                    }

                }

            }

        } catch (IOException e) {

        }

        return null;

    }

    public static int checkMapped(String checkAddress) {

        try {

            if (internetGatewayDevices == null) {
                internetGatewayDevices = InternetGatewayDevice.getDevices(SCAN_TIMEOUT);
            }

            if (internetGatewayDevices != null) {

                for (InternetGatewayDevice testIGD : internetGatewayDevices) {

                    Integer natTableSize = testIGD.getNatTableSize();

                    for (int j = 0; j < natTableSize; j++) {

                        ActionResponse mapEntry = testIGD.getGenericPortMappingEntry(j);

                        String internalClient = mapEntry.getOutActionArgumentValue("NewInternalClient");
                        String remoteHost = mapEntry.getOutActionArgumentValue("NewRemoteHost");
                        String internalPort = mapEntry.getOutActionArgumentValue("NewInternalPort");

                        if (internalClient != null && remoteHost != null && internalPort != null) {
                            if (internalClient.equals(checkAddress)) {
                                if (remoteHost.equals("")) {
                                    if (internalPort.equals("5987")) {
                                        return Integer.parseInt(mapEntry.getOutActionArgumentValue("NewExternalPort"));
                                    }
                                }
                            }
                        }

                    }

                }

            }

        } catch (IOException e) {

        } catch (UPNPResponseException e) {

        } catch (NumberFormatException e) {
        }

        return -1;

    }

    public static boolean mapPort(String externalRouterIP, int externalRouterPort, String internalIP, int internalPort, String description) {

        try {

            if (internetGatewayDevices == null) {
                internetGatewayDevices = InternetGatewayDevice.getDevices(SCAN_TIMEOUT);
            }

            if (internetGatewayDevices != null) {

                for (InternetGatewayDevice addIGD : internetGatewayDevices) {

                    addIGD.addPortMapping(description, externalRouterIP, internalPort, externalRouterPort, internalIP, 0, UDP_PROTOCOL);

                }

                return true;

            }

        } catch (IOException e) {

        } catch (UPNPResponseException e) {

        }

        return false;

    }

    public static boolean unmapPort(String externalIP, int port) throws IOException, UPNPResponseException {


        if (internetGatewayDevices == null) {
            internetGatewayDevices = InternetGatewayDevice.getDevices(SCAN_TIMEOUT);
        }

        if (internetGatewayDevices != null) {

            for (InternetGatewayDevice removeIGD : internetGatewayDevices) {

                removeIGD.deletePortMapping(externalIP, port, UDP_PROTOCOL);

            }

            return true;

        }

        return false;

    }

}