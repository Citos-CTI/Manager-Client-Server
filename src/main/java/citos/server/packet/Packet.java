/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.packet;

public class Packet {
    private final long arrivalTime;
    public Packet() {
        arrivalTime = System.currentTimeMillis();
    }
}
