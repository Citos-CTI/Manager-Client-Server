/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server.packet;

public class Packet {
    private final long arrivalTime;
    public Packet() {
        arrivalTime = System.currentTimeMillis();
    }
}
