package com.jaspergoes.bilight.milight.objects;

public class Device implements Comparable<Device> {

    public final String addrIP;
    public final String addrMAC;
    public final int addrPort;

    public Device(String addrIP, String addrMAC, int addrPort) {
        this.addrIP = addrIP;
        this.addrMAC = addrMAC;
        this.addrPort = addrPort;
    }

    @Override
    public int compareTo(Device compareTo) {
        return this.addrIP.compareTo(compareTo.addrIP);
    }

}
